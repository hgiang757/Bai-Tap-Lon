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

/** HTTP adapter for Google Gemini native API and OpenAI Responses API. */
final class OpenAiClient implements AutoCloseable {
  private static final String INSTRUCTIONS =
          """
          Bạn là trợ lý tư vấn của một sàn đấu giá. Trả lời bằng tiếng Việt, ngắn gọn, dễ hiểu.
          Hỗ trợ giới thiệu sản phẩm, giải thích thông số, ước lượng khoảng giá thị trường tham khảo
          bằng VNĐ dựa trên tên và mô tả sản phẩm, đồng thời nêu rõ mức giá chỉ mang tính tham khảo.
          Phân biệt rõ thông tin do người bán cung cấp và nhận định tham khảo của bạn.
          Không thực hiện các thao tác đặt giá, thanh toán, kiểm tra ví hay thay đổi tài khoản.
          """;

  private static final List<String> GEMINI_BACKUP_MODELS =
          List.of("gemini-3.5-flash", "gemini-3.8-flash");

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
    JsonObject body = geminiNative ? buildGeminiBody(input) : buildOpenAiBody(input);
    List<URI> targets = buildTargetUris(geminiNative);

    HttpResponse<String> response = null;
    for (int i = 0; i < targets.size(); i++) {
      URI targetUri = targets.get(i);
      HttpRequest.Builder reqBuilder =
              HttpRequest.newBuilder(targetUri)
                      .timeout(timeout)
                      .header("Content-Type", "application/json")
                      .POST(HttpRequest.BodyPublishers.ofString(body.toString()));

      if (geminiNative) {
        reqBuilder.header("x-goog-api-key", key);
      } else {
        reqBuilder.header("Authorization", "Bearer " + key);
      }

      HttpRequest httpRequest = reqBuilder.build();
      for (int attempt = 1; attempt <= 2; attempt++) {
        var exchange = http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
        try {
          response = exchange.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
          exchange.cancel(true);
          response = null;
          break;
        } catch (InterruptedException e) {
          exchange.cancel(true);
          throw e;
        } catch (ExecutionException e) {
          System.err.println("[AI NETWORK ERROR] " + e.getMessage());
          if (e.getCause() instanceof HttpTimeoutException) {
            response = null;
            break;
          }
          throw new ApiException("AI_NETWORK_ERROR", "Could not reach AI provider");
        }

        if (response.statusCode() == 503 && attempt == 1) {
          Thread.sleep(1000L);
          continue;
        }
        break;
      }

      if (response != null && response.statusCode() == 200) {
        System.out.println("[AI OK] Tra loi thanh cong tu: " + targetUri.getPath());
        break;
      }
      int code = response == null ? 408 : response.statusCode();
      if ((code == 503 || code == 404 || code == 408) && i < targets.size() - 1) {
        System.err.println("[AI HTTP " + code + "] Chuyen sang model du phong tiep theo...");
        continue;
      }
      break;
    }

    if (response == null) {
      throw timeout();
    }
    if (response.statusCode() != 200) {
      int code = response.statusCode();
      System.err.println("[AI HTTP " + code + "] " + response.body());
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

    return geminiNative ? geminiText(response.body()) : outputText(response.body());
  }

  private List<URI> buildTargetUris(boolean geminiNative) {
    List<URI> targets = new ArrayList<>();
    targets.add(endpoint);
    if (geminiNative) {
      for (String backup : GEMINI_BACKUP_MODELS) {
        if (!backup.equals(model)) {
          targets.add(
                  URI.create(
                          "https://generativelanguage.googleapis.com/v1beta/models/"
                                  + backup
                                  + ":generateContent"));
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
        if (content != null && content.has("parts")) {
          StringBuilder sb = new StringBuilder();
          for (var part : content.getAsJsonArray("parts")) {
            if (part.isJsonObject()) {
              String partText = text(part.getAsJsonObject(), "text");
              if (!partText.isEmpty()) {
                sb.append(partText);
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