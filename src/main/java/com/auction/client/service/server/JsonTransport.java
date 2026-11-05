package com.auction.client.service.server;

import com.auction.shared.JsonProtocol;
import com.google.gson.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** One reader per connection, correlated responses, independent ordered push dispatch. */
public final class JsonTransport implements AutoCloseable {
    public static final int MAX_FRAME = JsonProtocol.MAX_FRAME;
    private final String host;
    private final int port, timeout;
    private final Consumer<JsonObject> events;
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
            }
            pending.forEach((k, f) -> f.completeExceptionally(reason));
            pending.clear();
        }
    }

    public JsonTransport(String host, int port, int timeout, Consumer<JsonObject> events) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.events = events;
    }

    private synchronized Connection connection() {
        if (closed) throw new IllegalStateException("Kết nối đã đóng.");
        if (current != null && !current.socket.isClosed()) return current;
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
            }
            throw network(e);
        }
    }

    public long generation() {
        return connection().generation;
    }

    public JsonObject call(String action, String token, String commandId, JsonObject payload) {
        Connection c = connection();
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
        try {
            synchronized (c.out) {
                JsonProtocol.writeFrame(c.out, request);
            }
            return response.get(timeout, TimeUnit.MILLISECONDS);
        } catch (IOException | ExecutionException | TimeoutException e) {
            c.fail(e);
            throw network(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            c.fail(e);
            throw network(e);
        } finally {
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
                    if (waiter != null) waiter.complete(value);
                }
            }
        } catch (IOException | RuntimeException e) {
            c.fail(e);
        }
    }

    private IllegalStateException network(Exception cause) {
        return new IllegalStateException(
                "Không thể liên lạc máy chủ " + host + ":" + port + ". Kiểm tra server và kết nối.",
                cause);
    }

    public synchronized void disconnect() {
        if (current != null) current.fail(new IOException("Disconnected"));
        current = null;
    }

    @Override
    public synchronized void close() {
        closed = true;
        disconnect();
        dispatch.shutdownNow();
    }
}
