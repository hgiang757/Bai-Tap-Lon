package com.auction.server.assistant;

import static com.auction.shared.JsonProtocol.obj;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Loopback HTTP server that implements the relevant parts of the Responses API for tests. */
public final class MockOpenAi implements AutoCloseable {
  private final HttpServer server;
  private final java.util.concurrent.ExecutorService workers =
      Executors.newVirtualThreadPerTaskExecutor();
  private final BlockingQueue<Request> requests = new LinkedBlockingQueue<>();

  /** Captured HTTP metadata and API body. */
  public record Request(String authorization, JsonObject body) {
    /** Untrusted facts and question supplied to the model as a user message. */
    public JsonObject context() {
      return JsonParser.parseString(
              body.getAsJsonArray("input").get(0).getAsJsonObject().get("content").getAsString())
          .getAsJsonObject();
    }
  }

  /** Status and response body for the local provider. */
  public record Response(int status, String json) {}

  /** Produces a response and optionally delays it while the caller exercises the client. */
  @FunctionalInterface
  public interface Handler {
    /** Returns the response for the captured request. */
    Response respond(Request request) throws InterruptedException;
  }

  /** Starts the deterministic mock API on a random local port. */
  public MockOpenAi(Handler handler) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(workers);
    server.createContext(
        "/v1/responses",
        exchange -> {
          try (exchange) {
            String body =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var request =
                new Request(
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    JsonParser.parseString(body).getAsJsonObject());
            requests.add(request);
            Response response = handler.respond(request);
            byte[] bytes = response.json().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        });
    server.start();
  }

  /** Creates an assistant backed by this local server. */
  public AuctionAssistant assistant(Duration timeout) {
    return assistant("mock-key", timeout, product -> Optional.empty(), Clock.systemUTC());
  }

  /** Creates an assistant using local HTTP and the supplied environment equivalents. */
  public AuctionAssistant assistant(
      String key, Duration timeout, MarketPriceService market, Clock clock) {
    URI endpoint =
        URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses");
    return new AuctionAssistant(
        new OpenAiClient(key, "mock-model", endpoint, timeout), market, clock);
  }

  /** Returns the next captured request, or null if the wait expires. */
  public Request nextRequest(Duration wait) throws InterruptedException {
    return requests.poll(wait.toMillis(), TimeUnit.MILLISECONDS);
  }

  /** Encodes a completed Responses API message. */
  public static Response answer(String answer) {
    JsonArray content = new JsonArray();
    content.add(obj("type", "output_text", "text", answer));
    JsonArray output = new JsonArray();
    output.add(obj("type", "message", "content", content));
    return new Response(200, obj("status", "completed", "output", output).toString());
  }

  @Override
  public void close() {
    server.stop(0);
    workers.shutdownNow();
    workers.close();
  }
}
