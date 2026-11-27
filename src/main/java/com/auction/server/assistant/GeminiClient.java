package com.auction.server.assistant;

import static com.auction.shared.JsonProtocol.isString;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.parseObject;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.exception.ApiException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/** HTTP adapter for the Gemini GenerateContent API. */
final class GeminiClient implements AiProvider {
  private static final String KEY_HEADER = "x-goog-api-key";
  private static final Set<String> BLOCKED =
      Set.of("SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII");

  private final String key;
  private final URI endpoint;
  private final Duration timeout;
  private final AiHttp http;
  private final String configurationError;
  private final String keyProblem;

  GeminiClient(String key, URI endpoint, Duration timeout) {
    this.key = key == null ? "" : stripKeyEdges(key);
    this.endpoint = endpoint;
    this.timeout = timeout;
    keyProblem = keyProblem(this.key);
    configurationError = this.key.isEmpty() ? "AI_UNAVAILABLE"
        : keyProblem.isEmpty() ? "" : "AI_KEY_FORMAT";
    http = configurationError.isEmpty() ? new AiHttp(timeout) : null;
  }

  private static String stripKeyEdges(String value) {
    int start = 0;
    int end = value.length();
    while (start < end && isKeyEdge(value.charAt(start))) {
      start++;
    }
    while (end > start && isKeyEdge(value.charAt(end - 1))) {
      end--;
    }
    return value.substring(start, end);
  }

  private static boolean isKeyEdge(int character) {
    return character == 0xfeff || isWhitespace(character);
  }

  private static boolean isWhitespace(int character) {
    return character == ' ' || (character >= '\t' && character <= '\r')
        || character == 0x85 || Character.isSpaceChar(character);
  }

  private static String keyProblem(String key) {
    if (key.isEmpty()) {
      return "missing key";
    }
    if (key.codePoints().allMatch(c -> c == '*' || c == '•' || c == '●')) {
      return "only masking characters were received";
    }
    for (int offset = 0; offset < key.length(); ) {
      int character = key.codePointAt(offset);
      if (isWhitespace(character)
          || Character.isISOControl(character)
          || Character.getType(character) == Character.FORMAT) {
        return String.format(Locale.ROOT, "forbidden whitespace/control/format U+%04X", character);
      }
      offset += Character.charCount(character);
    }
    try {
      HttpRequest.newBuilder().header(KEY_HEADER, key);
      return "";
    } catch (IllegalArgumentException invalidHeader) {
      // The JDK exception can contain the entire header value.
      return "character unsupported by the HTTP client";
    }
  }

  @Override
  public boolean isReady() {
    return http != null;
  }

  @Override
  public String status() {
    return switch (configurationError) {
      case "AI_UNAVAILABLE" -> "Gemini: missing GEMINI_API_KEY";
      case "AI_KEY_FORMAT" ->
          "Gemini: GEMINI_API_KEY rejected locally at Java HTTP preflight (" + keyProblem + ")";
      default -> "Gemini: configured; authentication checked on request";
    };
  }

  @Override
  public String answer(JsonObject context) throws InterruptedException {
    if (http == null) {
      reportVerification("Gemini preflight: " + configurationError + " (" + keyProblem + ")");
      throw new ApiException(configurationError, "Gemini preflight: " + keyProblem);
    }
    JsonObject body =
        obj(
            "systemInstruction", obj("parts", array(obj("text", INSTRUCTIONS))),
            "contents", array(obj("role", "user", "parts", array(obj("text", context.toString())))),
            "generationConfig", obj("maxOutputTokens", 1024));
    HttpRequest request =
        HttpRequest.newBuilder(endpoint)
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .header(KEY_HEADER, key)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
    HttpResponse<String> response;
    try {
      response = http.post(request, true);
    } catch (ApiException e) {
      reportVerification("Gemini transport: " + e.code() + " (no HTTP response received)");
      throw e;
    }
    int status = response.statusCode();
    if (status == 200) {
      reportVerification("Gemini HTTP 200");
      return answerText(response.body());
    }
    String code = switch (status) {
      case 400 -> invalidKey(response.body()) ? "AI_AUTH_FAILED" : "AI_REQUEST_REJECTED";
      case 401 -> "AI_AUTH_FAILED";
      case 403 -> invalidKey(response.body()) ? "AI_AUTH_FAILED" : "AI_FORBIDDEN";
      case 404 -> "AI_MODEL_UNAVAILABLE";
      case 407 -> "AI_PROXY_ERROR";
      case 408, 504 -> "AI_TIMEOUT";
      case 429 -> "AI_RATE_LIMIT";
      case 500, 502, 503 -> "AI_PROVIDER_BUSY";
      default -> "AI_PROVIDER_ERROR";
    };
    reportVerification("Gemini HTTP " + status + " (" + code + ")");
    throw new ApiException(code, "Gemini HTTP " + status);
  }

  private static void reportVerification(String message) {
    if (Boolean.getBoolean("auction.ai.live")) {
      System.out.println(message);
    }
  }

  private static boolean invalidKey(String body) {
    try {
      JsonObject error = parseObject(body).getAsJsonObject("error");
      if (error == null) {
        return false;
      }
      JsonArray details = error.getAsJsonArray("details");
      return details != null && details.asList().stream()
          .filter(detail -> detail.isJsonObject())
          .map(detail -> text(detail.getAsJsonObject(), "reason"))
          .anyMatch(reason -> reason.equals("API_KEY_INVALID") || reason.equals("API_KEY_EXPIRED"));
    } catch (JsonParseException | ClassCastException | IllegalStateException e) {
      return false;
    }
  }

  private static String answerText(String body) {
    try {
      JsonObject root = parseObject(body);
      var feedback = root.getAsJsonObject("promptFeedback");
      if (feedback != null && !text(feedback, "blockReason").isBlank()) {
        throw new ApiException("AI_CONTENT_BLOCKED", "Gemini blocked the question");
      }
      var candidates = root.getAsJsonArray("candidates");
      if (candidates != null && !candidates.isEmpty()) {
        var candidate = candidates.get(0).getAsJsonObject();
        String reason = text(candidate, "finishReason");
        if (BLOCKED.contains(reason)) {
          throw new ApiException("AI_CONTENT_BLOCKED", "Gemini blocked the answer");
        }
        if (reason.equals("STOP") || reason.equals("MAX_TOKENS")) {
          var content = candidate.getAsJsonObject("content");
          StringBuilder answer = new StringBuilder();
          if (content != null && content.has("parts")) {
            for (var part : content.getAsJsonArray("parts")) {
              JsonObject value = part.getAsJsonObject();
              if (!"true".equals(text(value, "thought")) && isString(value.get("text"))) {
                answer.append(text(value, "text"));
              }
            }
          }
          if (!answer.toString().isBlank() && answer.length() <= 4_000) {
            return answer.toString().strip();
          }
        }
      }
    } catch (JsonParseException | ClassCastException | IllegalStateException
        | UnsupportedOperationException e) {
      throw new ApiException("AI_PROVIDER_ERROR", "Invalid Gemini response");
    }
    throw new ApiException("AI_PROVIDER_ERROR", "Gemini returned no visible answer");
  }

  private static JsonArray array(JsonObject value) {
    JsonArray values = new JsonArray();
    values.add(value);
    return values;
  }

  @Override
  public void close() {
    if (http != null) {
      http.close();
    }
  }
}
