package com.auction.server.assistant;

import static com.auction.shared.JsonProtocol.isString;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.parseObject;

import com.auction.server.exception.ApiException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

/** Optional local Ollama adapter. Requests use an already installed model on loopback. */
final class OllamaClient implements AiProvider {
  private final String model;
  private final URI endpoint;
  private final AiHttp http;

  OllamaClient(String model, URI endpoint, Duration timeout) {
    this.model = model == null ? "" : model.strip();
    this.endpoint = endpoint;
    http = this.model.isEmpty() ? null : new AiHttp(timeout);
  }

  @Override
  public String status() {
    return http == null ? "Ollama: missing OLLAMA_MODEL"
        : "Ollama: configured on loopback; connection checked on request";
  }

  @Override
  public String answer(JsonObject context) throws InterruptedException {
    if (http == null) {
      throw new ApiException("AI_OLLAMA_NOT_CONFIGURED", "OLLAMA_MODEL is missing");
    }
    JsonArray messages = new JsonArray();
    messages.add(obj("role", "system", "content", INSTRUCTIONS));
    messages.add(obj("role", "user", "content", context.toString()));
    var request = HttpRequest.newBuilder(endpoint)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(obj("model", model, "messages", messages,
            "stream", false, "options", obj("num_predict", 550)).toString()))
        .build();
    var response = http.post(request, false);
    if (response.statusCode() == 404) {
      throw new ApiException("AI_MODEL_UNAVAILABLE", "Ollama model or endpoint unavailable");
    }
    if (response.statusCode() != 200) {
      throw new ApiException("AI_PROVIDER_ERROR", "Ollama request failed");
    }
    try {
      var body = parseObject(response.body());
      var message = body.getAsJsonObject("message");
      var done = body.get("done");
      if (message != null && isString(message.get("content"))
          && done != null && done.isJsonPrimitive() && done.getAsJsonPrimitive().isBoolean()
          && done.getAsBoolean()) {
        String answer = message.get("content").getAsString().strip();
        if (!answer.isBlank() && answer.length() <= 4_000) {
          return answer;
        }
      }
    } catch (JsonParseException | ClassCastException e) {
      throw new ApiException("AI_PROVIDER_ERROR", "Invalid Ollama response");
    }
    throw new ApiException("AI_PROVIDER_ERROR", "Ollama returned no visible answer");
  }

  @Override
  public void close() {
    if (http != null) {
      http.close();
    }
  }
}
