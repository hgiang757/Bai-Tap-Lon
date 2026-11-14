package com.auction.client.service.server;

import com.auction.shared.JsonProtocol;
import com.google.gson.JsonObject;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
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

/** One reader per connection, correlated responses, independent ordered push dispatch. */
public final class JsonTransport implements AutoCloseable {
  public static final int MAX_FRAME = JsonProtocol.MAX_FRAME;
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
      } catch (IOException ignored) {
        // The connection is already failing or closing; there is no recovery here.
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
      } catch (IOException ignored) {
        // The connection is already failing or closing; there is no recovery here.
      }
      throw network(e);
    }
  }

  /** Returns the current connection generation for subscription recovery. */
  public long generation() {
    return connection().generation;
  }

  /** Sends a correlated request and waits within the configured network timeout. */
  public JsonObject call(String action, String token, String commandId, JsonObject payload) {
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
      return response.get(timeout, TimeUnit.MILLISECONDS);
    } catch (IOException | ExecutionException | TimeoutException | RejectedExecutionException e) {
      c.fail(e);
      throw network(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      c.fail(e);
      throw network(e);
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
        if ("event".equals(value.get("type").getAsString())) {
          dispatch.execute(
              () -> {
                try {
                  events.accept(value);
                } catch (RuntimeException e) {
                  System.err.println("Push: " + e.getMessage());
                }
              });
        } else {
          CompletableFuture<JsonObject> waiter =
              c.pending.get(value.get("requestId").getAsString());
          if (waiter != null) {
            waiter.complete(value);
          }
        }
      }
    } catch (IOException | RuntimeException e) {
      c.fail(e);
    }
  }

  private IllegalStateException network(Exception cause) {
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
