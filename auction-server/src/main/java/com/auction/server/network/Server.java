package com.auction.server.network;

import com.auction.server.application.*;
import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Framed TCP JSON, ordered bounded output per client, no I/O in transactions. */
public final class Server implements AutoCloseable {
    public static final int MAX_FRAME = 8 * 1024 * 1024;
    private final ServerSocket listener;
    private final AuctionApplication application;
    private final Set<Client> clients = ConcurrentHashMap.newKeySet();
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Thread acceptor;
    public Server(String host, int port, Path data, boolean demo) throws IOException {
        listener = new ServerSocket(); listener.setReuseAddress(true);
        try { listener.bind(new InetSocketAddress(host, port)); application = new AuctionApplication(data, Clock.system(AuctionApplication.ZONE), demo, this::publish); }
        catch (IOException | RuntimeException e) { listener.close(); workers.close(); scheduler.shutdownNow(); throw e; }
        acceptor = Thread.ofPlatform().name("auction-accept").unstarted(this::accept);
    }
    public int port() { return listener.getLocalPort(); }
    public void start() {
        acceptor.start();
        scheduler.scheduleAtFixedRate(() -> { try { application.tick(); } catch (RuntimeException e) { System.err.println("Scheduler: " + e.getMessage()); } }, 0, 250, TimeUnit.MILLISECONDS);
    }
    private void accept() {
        while (!closed.get()) try {
            Socket socket = listener.accept();
            if (clients.size() >= 200) { socket.close(); continue; }
            socket.setTcpNoDelay(true); socket.setSoTimeout(60_000);
            Client c = new Client(socket); clients.add(c); workers.submit(c::writeLoop); workers.submit(c::readLoop);
        } catch (IOException e) { if (!closed.get()) System.err.println("Accept: " + e.getMessage()); }
    }
    private void publish(JsonObject event) {
        for (Client c : clients) {
            String target = AuctionApplication.text(event, "userId"), auction = AuctionApplication.text(event, "auctionId");
            if (!target.isEmpty() ? target.equals(c.userId) : c.feed || auction.isEmpty() || c.subscriptions.contains(auction)) c.offer(event);
        }
    }
    public static JsonObject readFrame(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > MAX_FRAME) throw new IOException("Invalid frame size");
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) throw new EOFException();
        try { return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(); }
        catch (RuntimeException e) { throw new IOException("Malformed JSON object", e); }
    }
    public static void writeFrame(DataOutputStream out, JsonObject value) throws IOException {
        byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FRAME) throw new IOException("Response exceeds maximum frame size");
        out.writeInt(bytes.length); out.write(bytes); out.flush();
    }
    private final class Client implements AutoCloseable {
        private final Socket socket;
        private final BlockingQueue<JsonObject> outbound = new ArrayBlockingQueue<>(256);
        private final Set<String> subscriptions = ConcurrentHashMap.newKeySet();
        private volatile String userId = "", session = "";
        private volatile boolean done;
        private volatile boolean feed;
        Client(Socket socket) { this.socket = socket; }
        void offer(JsonObject value) { if (!done && !outbound.offer(value)) close(); }
        void writeLoop() {
            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                while (!done) { JsonObject next = outbound.poll(1, TimeUnit.SECONDS); if (next != null) writeFrame(out, next); }
            } catch (IOException | InterruptedException e) { if (e instanceof InterruptedException) Thread.currentThread().interrupt(); }
            finally { close(); }
        }
        void readLoop() {
            try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
                while (!done) {
                    JsonObject request = readFrame(in), response;
                    String requestId = AuctionApplication.text(request, "requestId");
                    try {
                        if (AuctionApplication.number(request, "protocolVersion") != 1 || requestId.isBlank() || requestId.length() > 100)
                            throw new ApiException("PROTOCOL_ERROR", "protocolVersion=1 and requestId required");
                        String action = AuctionApplication.text(request, "action"), token = AuctionApplication.text(request, "token");
                        JsonObject payload = request.has("payload") ? request.getAsJsonObject("payload") : new JsonObject();
                        if (!token.equals(session)) { subscriptions.clear(); feed = false; userId = ""; session = token; }
                        if (action.equals("SUBSCRIBE_FEED")) {
                            userId = application.authenticatedUserId(token); feed = true;
                            response = AuctionApplication.obj("success", true, "result", new JsonObject());
                        } else if (action.equals("SUBSCRIBE_AUCTION")) {
                            userId = application.authenticatedUserId(token);
                            String id = AuctionApplication.text(payload, "auctionId"); subscriptions.add(id);
                            response = application.execute("GET_AUCTION", token, "", payload);
                            if (!response.get("success").getAsBoolean()) subscriptions.remove(id);
                        } else if (action.equals("UNSUBSCRIBE_AUCTION")) {
                            subscriptions.remove(AuctionApplication.text(payload, "auctionId")); response = AuctionApplication.obj("success", true, "result", new JsonObject());
                        } else response = application.execute(action, token, AuctionApplication.text(request, "commandId"), payload);
                        if (action.equals("LOGIN") && response.get("success").getAsBoolean()) {
                            session = AuctionApplication.text(response.getAsJsonObject("result"), "token"); userId = application.authenticatedUserId(session);
                        } else if (action.equals("LOGOUT")) { subscriptions.clear(); feed = false; userId = ""; session = ""; }
                        else if (!token.isBlank()) userId = application.authenticatedUserId(token);
                    } catch (ApiException e) { response = AuctionApplication.obj("success", false, "errorCode", e.code(), "message", e.getMessage()); }
                    catch (RuntimeException e) { response = AuctionApplication.obj("success", false, "errorCode", "INVALID_REQUEST", "message", "Malformed request"); }
                    response.addProperty("type", "response"); response.addProperty("protocolVersion", 1); response.addProperty("requestId", requestId); offer(response);
                }
            } catch (IOException e) { /* Release disconnected and idle clients. */ }
            finally { close(); }
        }
        @Override public void close() { done = true; clients.remove(this); subscriptions.clear(); try { socket.close(); } catch (IOException ignored) { } }
    }
    @Override public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) return;
        listener.close(); scheduler.shutdownNow(); clients.forEach(Client::close); workers.shutdownNow();
        try { scheduler.awaitTermination(5, TimeUnit.SECONDS); workers.awaitTermination(5, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        application.close();
    }
    public static void main(String[] args) throws Exception {
        Server server = new Server(System.getProperty("auction.host", "127.0.0.1"), Integer.getInteger("auction.port", 8888),
                Path.of(System.getProperty("auction.data", "data")), Boolean.getBoolean("auction.demo"));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { server.close(); } catch (IOException e) { System.err.println(e.getMessage()); } }));
        server.start(); System.out.println("Auction JSON server listening on " + server.listener.getLocalSocketAddress()); server.acceptor.join();
    }
}
