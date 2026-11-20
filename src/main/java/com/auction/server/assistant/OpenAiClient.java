package com.auction.server.assistant;

import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.exception.ApiException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Giao tiếp HTTP với API Gemini và OpenAI Responses. */
final class OpenAiClient implements AutoCloseable {
  private static final String INSTRUCTIONS =
      """
      Bạn là trợ lý tư vấn của một sàn đấu giá. Trả lời ngắn gọn bằng tiếng Việt.
      Giải thích thông tin sản phẩm và phân biệt mô tả của người bán với nhận xét của bạn.
      Giá phiên và giá thị trường chỉ được hiển thị từ dữ liệu do máy chủ cung cấp.
      Khi được hỏi về giá hoặc nguồn, hướng người dùng đến phần thông tin dưới câu trả lời.
      Không tự ước lượng giá, tạo đường dẫn, đặt giá, thanh toán hay thay đổi tài khoản.
      """;

  private static final List<String> GEMINI_BACKUP_MODELS =
      List.of("gemini-3.5-flash", "gemini-3.8-flash");
  private static final Duration RETRY_DELAY = Duration.ofSeconds(1);

  private final String key;
  private final String model;
  private final URI endpoint;
  private final Duration timeout;
  private final HttpClient http;

  OpenAiClient(String key, String model, URI endpoint, Duration timeout) {
    this.key = key == null ? "" : key.strip();
    this.model = model;
    this.endpoint = endpoint;
    this.timeout = timeout;
    http = this.key.isEmpty() ? null : HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  void requireConfigured() {
    if (http == null) {
      throw new ApiException("AI_UNAVAILABLE", "AI API key is missing");
    }
  }

  String answer(JsonArray input) throws InterruptedException {
    requireConfigured();
    boolean geminiNative = isGeminiNative();
    String body = (geminiNative ? buildGeminiBody(input) : buildOpenAiBody(input)).toString();
    long deadline = System.nanoTime() + timeout.toNanos();
    var targets = buildTargetUris(geminiNative).iterator();
    HttpResponse<String> response;
    do {
      URI target = targets.next();
      response = send(target, body, geminiNative, deadline);
      if (response.statusCode() == 503 && deadline - System.nanoTime() > RETRY_DELAY.toNanos()) {
        Thread.sleep(RETRY_DELAY);
        response = send(target, body, geminiNative, deadline);
      }
    } while (targets.hasNext() && (response.statusCode() == 503 || response.statusCode() == 404));
    if (response.statusCode() == 200) {
      return geminiNative ? geminiText(response.body()) : outputText(response.body());
    }

    int code = response.statusCode();
    System.err.println("[AI HTTP " + code + "]");
    if (code == 429) {
      if ("insufficient_quota".equals(providerError(response.body()))) {
        throw new ApiException("AI_QUOTA", "AI quota exhausted");
      }
      throw new ApiException("AI_RATE_LIMIT", "AI rate limit exceeded");
    }
    if (code == 401 || code == 403) {
      throw new ApiException("AI_UNAVAILABLE", "AI authorization failed");
    }
    throw new ApiException("AI_PROVIDER_ERROR", "AI request failed");
  }

  private HttpResponse<String> send(URI target, String body, boolean geminiNative, long deadline)
      throws InterruptedException {
    long remaining = deadline - System.nanoTime();
    if (remaining <= 0) {
      throw timeout();
    }
    HttpRequest request =
        HttpRequest.newBuilder(target)
            .timeout(Duration.ofNanos(remaining))
            .header("Content-Type", "application/json")
            .header(
                geminiNative ? "x-goog-api-key" : "Authorization",
                geminiNative ? key : "Bearer " + key)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var exchange = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    try {
      return exchange.get(remaining, TimeUnit.NANOSECONDS);
    } catch (TimeoutException e) {
      exchange.cancel(true);
      throw timeout();
    } catch (InterruptedException e) {
      exchange.cancel(true);
      throw e;
    } catch (ExecutionException e) {
      if (e.getCause() instanceof HttpTimeoutException) {
        throw timeout();
      }
      throw new ApiException("AI_NETWORK_ERROR", "Could not reach AI provider");
    }
  }

  private List<URI> buildTargetUris(boolean geminiNative) {
    List<URI> targets = new ArrayList<>();
    targets.add(endpoint);
    if (geminiNative) {
      for (String backup : GEMINI_BACKUP_MODELS) {
        if (!backup.equals(model)) {
          targets.add(endpoint.resolve("./" + backup + ":generateContent"));
        }
      }
    }
    return targets;
  }

  private boolean isGeminiNative() {
    return endpoint != null
        && endpoint.getPath() != null
        && endpoint.getPath().endsWith(":generateContent");
  }

  private JsonObject buildOpenAiBody(JsonArray input) {
    return obj(
        "model", model,
        "instructions", INSTRUCTIONS,
        "input", input,
        "store", false,
        "max_output_tokens", 450);
  }

  private JsonObject buildGeminiBody(JsonArray input) {
    JsonArray sysParts = new JsonArray();
    sysParts.add(obj("text", INSTRUCTIONS));
    JsonObject systemInstruction = obj("parts", sysParts);

    JsonArray contents = new JsonArray();
    if (input != null) {
      for (var element : input) {
        if (!element.isJsonObject()) {
          continue;
        }
        JsonObject item = element.getAsJsonObject();
        String role = "assistant".equals(text(item, "role")) ? "model" : "user";
        String contentText = extractInputContent(item);
        if (!contentText.isEmpty()) {
          JsonArray parts = new JsonArray();
          parts.add(obj("text", contentText));
          contents.add(obj("role", role, "parts", parts));
        }
      }
    }

    return obj(
        "systemInstruction", systemInstruction,
        "contents", contents,
        "generationConfig", obj("maxOutputTokens", 600));
  }

  private static String extractInputContent(JsonObject item) {
    if (!item.has("content") || item.get("content").isJsonNull()) {
      return "";
    }
    if (item.get("content").isJsonPrimitive()) {
      return item.get("content").getAsString();
    }
    if (item.get("content").isJsonArray()) {
      StringBuilder sb = new StringBuilder();
      for (var part : item.getAsJsonArray("content")) {
        if (part.isJsonObject()) {
          String partText = text(part.getAsJsonObject(), "text");
          if (!partText.isEmpty()) {
            if (!sb.isEmpty()) {
              sb.append('\n');
            }
            sb.append(partText);
          }
        }
      }
      return sb.toString();
    }
    return "";
  }

  private static ApiException timeout() {
    return new ApiException("AI_TIMEOUT", "AI response timed out");
  }

  private static String providerError(String body) {
    try {
      return text(JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("error"), "code");
    } catch (RuntimeException e) {
      return "";
    }
  }

  private static String geminiText(String body) {
    try {
      JsonObject root = JsonParser.parseString(body).getAsJsonObject();
      JsonArray candidates = root.getAsJsonArray("candidates");
      if (candidates != null && !candidates.isEmpty()) {
        JsonObject first = candidates.get(0).getAsJsonObject();
        JsonObject content = first.getAsJsonObject("content");
        if ("STOP".equals(text(first, "finishReason")) && content != null && content.has("parts")) {
          StringBuilder sb = new StringBuilder();
          for (var part : content.getAsJsonArray("parts")) {
            if (part.isJsonObject()) {
              JsonObject segment = part.getAsJsonObject();
              if (!segment.has("thought") || !segment.get("thought").getAsBoolean()) {
                sb.append(text(segment, "text"));
              }
            }
          }
          String answer = sb.toString().strip();
          if (!answer.isEmpty() && answer.length() <= 4_000) {
            return answer;
          }
        }
      }
    } catch (RuntimeException e) {
      throw new ApiException("AI_PROVIDER_ERROR", "Invalid Gemini response");
    }
    throw new ApiException("AI_PROVIDER_ERROR", "Gemini returned no complete answer");
  }

  private static String outputText(String body) {
    try {
      JsonObject root = JsonParser.parseString(body).getAsJsonObject();
      StringBuilder answer = new StringBuilder();
      if ("completed".equals(text(root, "status"))) {
        for (var item : root.getAsJsonArray("output")) {
          JsonObject message = item.getAsJsonObject();
          if ("message".equals(text(message, "type"))) {
            for (var part : message.getAsJsonArray("content")) {
              JsonObject content = part.getAsJsonObject();
              if ("output_text".equals(text(content, "type"))) {
                answer.append(text(content, "text")).append('\n');
              }
            }
          }
        }
      }
      if (!answer.toString().isBlank() && answer.length() <= 4_000) {
        return answer.toString().strip();
      }
    } catch (RuntimeException e) {
      throw new ApiException("AI_PROVIDER_ERROR", "Invalid OpenAI response");
    }
    throw new ApiException("AI_PROVIDER_ERROR", "AI provider returned no complete answer");
  }

  @Override
  public void close() {
    if (http != null) {
      http.shutdownNow();
    }
  }
}
