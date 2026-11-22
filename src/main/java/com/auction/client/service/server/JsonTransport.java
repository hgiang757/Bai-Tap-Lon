package com.auction.client.service.server;

import static com.auction.shared.JsonProtocol.isString;
import static com.auction.shared.JsonProtocol.text;

import com.auction.shared.JsonProtocol;
import com.google.gson.JsonObject;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** One reader per connection, correlated responses, independent ordered push dispatch. */
public final class JsonTransport implements AutoCloseable {
  public static final int MAX_FRAME = JsonProtocol.MAX_FRAME;
  private static final Logger LOGGER = Logger.getLogger(JsonTransport.class.getName());
  private final String host;
  private final int port;
  private final int timeout;
  private final Consumer<JsonObject> events;
  private final ScheduledThreadPoolExecutor deadlines =
      new ScheduledThreadPoolExecutor(
          1,
          runnable -> {
            Thread thread = new Thread(runnable, "auction-request-timeout");
            thread.setDaemon(true);
            return thread;
          });
  private final ExecutorService dispatch =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "auction-events");
            t.setDaemon(true);
            return t;
          });
  private Connection current;
  private long sequence;
  private boolean closed;

  private final class Connection {
    final Socket socket;
    final DataOutputStream out;
    final Map<String, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    final long generation;

    Connection(Socket socket) throws IOException {
      this.socket = socket;
      out = new DataOutputStream(socket.getOutputStream());
      generation = ++sequence;
    }

    void fail(Exception reason) {
      try {
        socket.close();
      } catch (IOException closeError) {
        reason.addSuppressed(closeError);
        LOGGER.log(Level.FINE, "Không đóng được kết nối cũ.", closeError);
      }
      pending.forEach((k, f) -> f.completeExceptionally(reason));
      pending.clear();
    }
  }

  /** Configures TCP requests and event dispatch without connecting immediately. */
  public JsonTransport(String host, int port, int timeout, Consumer<JsonObject> events) {
    this.host = host;
    this.port = port;
    this.timeout = timeout;
    this.events = events;
    deadlines.setRemoveOnCancelPolicy(true);
  }

  private synchronized Connection connection() {
    if (closed) {
      throw new IllegalStateException("Kết nối đã đóng.");
    }
    if (current != null && !current.socket.isClosed()) {
      return current;
    }
    Socket socket = new Socket();
    try {
      socket.connect(new InetSocketAddress(host, port), timeout);
      socket.setTcpNoDelay(true);
      Connection c = new Connection(socket);
      current = c;
      Thread.ofVirtual().name("auction-reader").start(() -> read(c));
      return c;
    } catch (IOException e) {
      try {
        socket.close();
      } catch (IOException closeError) {
        e.addSuppressed(closeError);
      }
      throw network(e, true);
    }
  }

  /** Returns the current connection generation for subscription recovery. */
  public long generation() {
    return connection().generation;
  }

  /** Sends a correlated request and waits within the configured network timeout. */
  public JsonObject call(String action, String token, String commandId, JsonObject payload) {
    return call(action, token, commandId, payload, null);
  }

  /**
   * Allows a distinct reply timeout for an isolated read. Once written, expiry only abandons that
   * request. A stalled write still closes the connection.
   */
  JsonObject call(
      String action, String token, String commandId, JsonObject payload, Duration replyTimeout) {
    final Connection c = connection();
    String requestId = UUID.randomUUID().toString();
    JsonObject request = new JsonObject();
    request.addProperty("protocolVersion", 1);
    request.addProperty("requestId", requestId);
    request.addProperty("action", action);
    request.addProperty("token", token);
    request.addProperty("commandId", commandId);
    request.add("payload", payload);
    CompletableFuture<JsonObject> response = new CompletableFuture<>();
    c.pending.put(requestId, response);
    ScheduledFuture<?> deadline = null;
    try {
      // Socket writes can block too; closing the socket also unblocks a stalled writer.
      deadline =
          deadlines.schedule(
              () -> {
                if (!response.isDone()) {
                  c.fail(new SocketTimeoutException("Request timed out"));
                }
              },
              timeout,
              TimeUnit.MILLISECONDS);
      synchronized (c.out) {
        JsonProtocol.writeFrame(c.out, request);
      }
      if (replyTimeout != null) {
        deadline.cancel(false);
        return response.get(replyTimeout.toMillis(), TimeUnit.MILLISECONDS);
      }
      return response.get(timeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      if (replyTimeout != null) {
        throw new ServerError("AI_TIMEOUT", "", e);
      }
      c.fail(e);
      throw network(e, false);
    } catch (IOException | ExecutionException | RejectedExecutionException e) {
      c.fail(e);
      throw network(e, false);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      c.fail(e);
      throw network(e, false);
    } finally {
      if (deadline != null) {
        deadline.cancel(false);
      }
      c.pending.remove(requestId);
    }
  }

  private void read(Connection c) {
    try (DataInputStream in = new DataInputStream(c.socket.getInputStream())) {
      while (!c.socket.isClosed()) {
        JsonObject value = JsonProtocol.readFrame(in);
        if (isString(value.get("type"))
            && "event".equals(text(value, "type"))
            && isString(value.get("event"))) {
          dispatch.execute(
              () -> {
                if (c.socket.isClosed()) {
                  return;
                }
                try {
                  events.accept(value);
                } catch (RuntimeException e) {
                  System.err.println("Push: " + e.getMessage());
                }
              });
        } else {
          validateResponse(value);
          CompletableFuture<JsonObject> waiter = c.pending.get(text(value, "requestId"));
          if (waiter != null) {
            waiter.complete(value);
          }
        }
      }
    } catch (IOException | RejectedExecutionException e) {
      c.fail(e);
    }
  }

  private static void validateResponse(JsonObject value) throws IOException {
    var success = value.get("success");
    if (!isString(value.get("type"))
        || !"response".equals(text(value, "type"))
        || !isString(value.get("requestId"))
        || text(value, "requestId").isBlank()
        || success == null
        || !success.isJsonPrimitive()
        || !success.getAsJsonPrimitive().isBoolean()
        || (success.getAsBoolean()
            ? !value.has("result")
            : !isString(value.get("errorCode")) || !isString(value.get("message")))) {
      throw new ProtocolException("Malformed response envelope");
    }
  }

  private IllegalStateException network(Exception cause, boolean connecting) {
    Throwable underlying = cause instanceof ExecutionException ? cause.getCause() : cause;
    if (underlying instanceof SocketTimeoutException || underlying instanceof TimeoutException) {
      return new IllegalStateException(
          connecting ? "Hết thời gian kết nối máy chủ." : "Máy chủ phản hồi quá thời gian.", cause);
    }
    if (underlying instanceof ProtocolException) {
      return new IllegalStateException("Phản hồi của máy chủ không đúng định dạng.", cause);
    }
    return new IllegalStateException(
        "Không thể liên lạc máy chủ " + host + ":" + port + ". Kiểm tra server và kết nối.", cause);
  }

  /** Closes the active connection and fails its outstanding requests. */
  public synchronized void disconnect() {
    if (current != null) {
      current.fail(new IOException("Disconnected"));
    }
    current = null;
  }

  @Override
  public synchronized void close() {
    closed = true;
    disconnect();
    deadlines.shutdownNow();
    dispatch.shutdownNow();
  }
}
