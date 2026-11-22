package com.auction.server.assistant;

import static com.auction.shared.JsonProtocol.obj;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OllamaClientTest {
  @Test
  void postsNonStreamingChatToLoopbackWithoutGoogleCredentials() throws Exception {
    var body = new AtomicReference<JsonObject>();
    var authorization = new AtomicReference<String>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/chat", exchange -> {
      try (exchange) {
        authorization.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
        body.set(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8)).getAsJsonObject());
        byte[] json = obj("done", true, "message", obj("content", "Đã đọc thông tin phiên."))
            .toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, json.length);
        exchange.getResponseBody().write(json);
      }
    });
    server.start();
    URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/chat");
    try (OllamaClient client = new OllamaClient("local-test-model", endpoint,
        Duration.ofSeconds(5))) {
      assertEquals("Đã đọc thông tin phiên.", client.answer(obj("question", "Giá hiện tại?")));
      assertNull(authorization.get());
      assertEquals("local-test-model", body.get().get("model").getAsString());
      assertFalse(body.get().get("stream").getAsBoolean());
      assertEquals(2, body.get().getAsJsonArray("messages").size());
    } finally {
      server.stop(0);
    }
  }
}
