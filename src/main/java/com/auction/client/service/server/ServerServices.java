package com.auction.client.service.server;

import static com.auction.client.service.server.ServerMapper.*;
import static com.auction.shared.JsonProtocol.obj;

import com.auction.client.model.*;
import com.auction.client.service.*;
import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** All six adapters share one authenticated transport; never falls back to mock. */
public final class ServerServices
        implements AuctionClientService,
                WalletService,
                AutoBidService,
                ProductService,
                UserAdminService,
                AuctionAdminService,
                AutoCloseable {
    private final JsonTransport transport;
    private final Path directory;
    private final Object writes = new Object(), subscriptions = new Object();
    private final Object imageCache = new Object();
    private final Map<String, CopyOnWriteArrayList<Consumer<Bid>>> bidListeners =
            new ConcurrentHashMap<>();
    private final List<Consumer<String>> changes = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService heartbeat =
            Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread t = new Thread(r, "auction-heartbeat");
                        t.setDaemon(true);
                        return t;
                    });
    private volatile String token = "";
    private volatile User currentUser;
    private volatile long subscribedGeneration = -1;
    private volatile Duration clockOffset = Duration.ZERO;
    private volatile boolean connected, closed;

    public ServerServices() {
        this(
                System.getProperty("auction.host", "127.0.0.1"),
                Integer.getInteger("auction.port", 8888),
                Path.of(
                        System.getProperty(
                                "auction.client.data",
                                Path.of(
                                                System.getProperty("user.home"),
                                                ".digital-auction",
                                                "server")
                                        .toString())));
    }

    public ServerServices(String host, int port, Path directory) {
        this.directory = directory.resolve(host.replaceAll("[^A-Za-z0-9_-]", "_") + "-" + port);
        transport =
                new JsonTransport(
                        host, port, Integer.getInteger("auction.timeout", 8000), this::onEvent);
        heartbeat.scheduleWithFixedDelay(
                () -> {
                    if (token.isEmpty() || closed) return;
                    try {
                        read("PING", obj());
                    } catch (RuntimeException e) {
                        if (connected) {
                            connected = false;
                            changed("DISCONNECTED");
                        }
                    }
                },
                5,
                5,
                TimeUnit.SECONDS);
    }

    private JsonObject response(String action, String command, JsonObject payload) {
        String session = token;
        if (!session.isEmpty())
            synchronized (subscriptions) {
                long generation = transport.generation();
                if (generation != subscribedGeneration) {
                    unwrap(transport.call("SUBSCRIBE_FEED", session, "", obj()), session);
                    subscribedGeneration = generation;
                    changed("RECONNECTED");
                }
            }
        JsonObject response = transport.call(action, session, command, payload);
        if (response.has("serverTime"))
            clockOffset =
                    Duration.between(
                            LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")),
                            LocalDateTime.parse(s(response, "serverTime")));
        connected = true;
        return response;
    }

    private JsonElement unwrap(JsonObject response, String session) {
        if (!response.get("success").getAsBoolean()) {
            String code = s(response, "errorCode");
            if (code.equals("UNAUTHENTICATED") && session.equals(token)) {
                token = "";
                currentUser = null;
                subscribedGeneration = -1;
                changed("SESSION_EXPIRED");
            }
            throw new ServerError(code, s(response, "message"));
        }
        return response.get("result");
    }

    private JsonElement read(String action, JsonObject payload) {
        String session = token;
        return unwrap(response(action, "", payload), session);
    }

    private Path pendingFile() {
        User user = currentUser;
        if (user == null) throw new ServerError("UNAUTHENTICATED", "");
        return directory.resolve("pending-" + user.getId() + ".json");
    }

    private JsonObject loadPending() {
        try {
            Path file = pendingFile();
            return Files.exists(file)
                    ? JsonParser.parseString(Files.readString(file)).getAsJsonObject()
                    : null;
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được lệnh đang chờ đối soát.", e);
        }
    }

    private record CommandLock(
            java.nio.channels.FileChannel channel, java.nio.channels.FileLock lock)
            implements AutoCloseable {
        @Override
        public void close() throws IOException {
            try {
                lock.release();
            } finally {
                channel.close();
            }
        }
    }

    private CommandLock lockPending() {
        try {
            Path file = pendingFile();
            Files.createDirectories(file.getParent());
            var channel =
                    java.nio.channels.FileChannel.open(
                            file.resolveSibling(file.getFileName() + ".lock"),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE);
            try {
                var lock = channel.tryLock();
                if (lock == null)
                    throw new ServerError(
                            "CONFLICT", "Tài khoản này đang gửi lệnh từ một cửa sổ khác.");
                return new CommandLock(channel, lock);
            } catch (IOException | RuntimeException e) {
                channel.close();
                throw e;
            }
        } catch (java.nio.channels.OverlappingFileLockException e) {
            throw new ServerError("CONFLICT", "Một cửa sổ khác đang đối soát lệnh.");
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Không khóa được sổ lệnh cục bộ; thao tác chưa gửi.", e);
        }
    }

    private void storePending(JsonObject command) {
        try {
            Path file = pendingFile();
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            try (var channel =
                    java.nio.channels.FileChannel.open(
                            temp,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE)) {
                var bytes =
                        java.nio.ByteBuffer.wrap(
                                command.toString().getBytes(StandardCharsets.UTF_8));
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(
                    temp,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Không lưu được mã lệnh; thao tác chưa được gửi.", e);
        }
    }

    private void clearPending() {
        try {
            Files.deleteIfExists(pendingFile());
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Không xóa được lệnh đã đối soát; vui lòng tải lại.", e);
        }
    }

    private JsonObject replay(JsonObject command) {
        RuntimeException last = null;
        for (int attempt = 0; attempt < 2; attempt++)
            try {
                return response(
                        s(command, "action"),
                        s(command, "commandId"),
                        command.getAsJsonObject("payload"));
            } catch (ServerError e) {
                throw e;
            } catch (IllegalStateException e) {
                last = e;
                transport.disconnect();
                subscribedGeneration = -1;
            }
        throw last;
    }

    private JsonElement write(String action, JsonObject payload) {
        synchronized (writes) {
            try (var ignored = lockPending()) {
                JsonObject previous = loadPending();
                if (previous != null) {
                    resolve(previous);
                    throw new IllegalStateException(
                            "Lệnh trước đã được đối soát. Hãy tải lại dữ liệu rồi thực hiện thao"
                                + " tác mới.");
                }
                JsonObject command =
                        obj("action", action, "commandId", UUID.randomUUID().toString());
                command.add("payload", payload.deepCopy());
                storePending(command);
                JsonObject result = replay(command);
                if (!result.get("success").getAsBoolean()
                        && s(result, "errorCode").equals("UNAUTHENTICATED"))
                    return unwrap(result, token);
                clearPending();
                return unwrap(result, token);
            } catch (IOException e) {
                throw new IllegalStateException("Không đóng được sổ lệnh.", e);
            }
        }
    }

    private void resolve(JsonObject pending) {
        JsonObject result = replay(pending);
        if (!result.get("success").getAsBoolean()
                && s(result, "errorCode").equals("UNAUTHENTICATED")) unwrap(result, token);
        clearPending();
        changed("COMMAND_RESOLVED");
    }

    @Override
    public boolean recoverPendingCommands() {
        synchronized (writes) {
            try (var ignored = lockPending()) {
                JsonObject pending = loadPending();
                if (pending != null) resolve(pending);
                return true;
            } catch (IOException e) {
                throw new IllegalStateException("Không đóng được sổ lệnh.", e);
            }
        }
    }

    @Override
    public boolean login(String username, String password, UserRole ignored) {
        synchronized (writes) {
            try {
                JsonObject result =
                        read("LOGIN", obj("username", username, "password", password))
                                .getAsJsonObject();
                token = s(result, "token");
                currentUser = user(result.getAsJsonObject("user"));
                subscribedGeneration = -1;
                read("GET_ME", obj());
                changed("SIGNED_IN");
                return true;
            } catch (ServerError e) {
                if (e.code().equals("AUTHENTICATION_FAILED")) return false;
                throw e;
            }
        }
    }

    @Override
    public boolean register(
            String username, String password, String fullName, String email, UserRole role) {
        JsonObject p =
                obj(
                        "username",
                        username,
                        "password",
                        password,
                        "fullName",
                        fullName,
                        "email",
                        email,
                        "role",
                        role);
        String command = UUID.randomUUID().toString();
        try {
            unwrap(response("REGISTER", command, p), token);
            return login(username, password, null);
        } catch (ServerError e) {
            if (e.code().equals("CONFLICT")) return false;
            throw e;
        } catch (
                IllegalStateException
                        e) { // A lost registration response can be resolved by authenticating that
                             // account.
            if (login(username, password, null)) return true;
            throw e;
        }
    }

    @Override
    public User getCurrentUser() {
        return currentUser;
    }

    @Override
    public void logout() {
        synchronized (writes) {
            try {
                if (!token.isEmpty()) read("LOGOUT", obj());
            } catch (IllegalStateException ignored) {
                /* Local logout must remain available when the server is offline. */
            } finally {
                token = "";
                currentUser = null;
                bidListeners.clear();
                transport.disconnect();
                subscribedGeneration = -1;
                changed("SIGNED_OUT");
            }
        }
    }

    @Override
    public LocalDateTime serverNow() {
        return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plus(clockOffset);
    }

    private List<Auction> auctions(JsonElement data) {
        List<Auction> out = new ArrayList<>();
        for (JsonElement a : data.getAsJsonArray()) out.add(auction(a.getAsJsonObject()));
        return out;
    }

    @Override
    public List<Auction> getAuctions(AuctionStatus filter) {
        return auctions(
                read("LIST_AUCTIONS", filter == null ? obj() : obj("status", filter.name())));
    }

    @Override
    public Auction getAuctionById(String id) {
        return auction(read("GET_AUCTION", obj("auctionId", id)).getAsJsonObject());
    }

    @Override
    public AuctionRoomSnapshot getRoomSnapshot(String id) {
        JsonObject o = read("GET_ROOM_SNAPSHOT", obj("auctionId", id)).getAsJsonObject();
        List<Bid> bids = new ArrayList<>();
        for (JsonElement b : o.getAsJsonArray("bids")) bids.add(bid(b.getAsJsonObject()));
        return new AuctionRoomSnapshot(auction(o.getAsJsonObject("auction")), bids);
    }

    @Override
    public List<Bid> getBidHistory(String id) {
        List<Bid> out = new ArrayList<>();
        for (JsonElement b : read("GET_BID_HISTORY", obj("auctionId", id)).getAsJsonArray())
            out.add(bid(b.getAsJsonObject()));
        return out;
    }

    @Override
    public BidResult placeBid(String id, double amount) {
        try {
            JsonObject result =
                    write("PLACE_BID", obj("auctionId", id, "amount", integer(amount)))
                            .getAsJsonObject();
            JsonArray history = result.getAsJsonArray("bids");
            Bid manual = null;
            for (JsonElement v : history) {
                Bid b = bid(v.getAsJsonObject());
                if (!b.isAuto()
                        && b.getBidderUsername().equals(currentUser.getUsername())
                        && b.getAmount() == amount) manual = b;
            }
            return BidResult.ok(
                    "Đặt giá thành công.",
                    n(result.getAsJsonObject("auction"), "currentPrice"),
                    manual);
        } catch (ServerError e) {
            return BidResult.fail(e.getMessage());
        }
    }

    private long integer(double value) {
        if (!Double.isFinite(value)
                || value <= 0
                || value != Math.rint(value)
                || value > 9_000_000_000_000L)
            throw new ServerError(
                    "VALIDATION_ERROR", "Số tiền phải là số nguyên dương trong giới hạn cho phép.");
        return (long) value;
    }

    private void self(String username) {
        if (currentUser == null || !currentUser.getUsername().equalsIgnoreCase(username))
            throw new ServerError("FORBIDDEN", "");
    }

    @Override
    public List<Auction> listProducts(String seller) {
        self(seller);
        return auctions(read("LIST_MY_PRODUCTS", obj()));
    }

    @Override
    public void saveProduct(Auction a, String imagePath) {
        JsonObject p =
                obj(
                        "name",
                        a.getItemName(),
                        "description",
                        a.getItem().getDescription(),
                        "category",
                        ItemCategory.from(a.getItemCategory()).name(),
                        "startingPrice",
                        integer(a.getStartingPrice()),
                        "minBidStep",
                        integer(a.getMinBidStep()),
                        "startTime",
                        a.getStartTime(),
                        "endTime",
                        a.getEndTime());
        // A UUID generated by the UI is only a draft ID; check existence in our server-owned
        // product list.
        boolean exists =
                listProducts(currentUser.getUsername()).stream()
                        .anyMatch(old -> old.getId().equals(a.getId()));
        if (exists) p.addProperty("auctionId", a.getId());
        if (imagePath != null)
            try {
                byte[] bytes =
                        imagePath.isBlank() ? new byte[0] : Files.readAllBytes(Path.of(imagePath));
                if (bytes.length >= 5 * 1024 * 1024)
                    throw new IOException("Ảnh phải nhỏ hơn 5 MB.");
                p.addProperty("imageBase64", Base64.getEncoder().encodeToString(bytes));
            } catch (IOException e) {
                throw new IllegalStateException("Không đọc được ảnh sản phẩm.", e);
            }
        write(exists ? "UPDATE_PRODUCT_AUCTION" : "CREATE_PRODUCT_AUCTION", p);
    }

    @Override
    public void deleteProduct(String seller, String id) {
        self(seller);
        write("DELETE_PRODUCT_AUCTION", obj("auctionId", id));
    }

    @Override
    public String getImagePath(String id) {
        JsonObject image = read("GET_IMAGE", obj("auctionId", id)).getAsJsonObject();
        String data = s(image, "imageBase64");
        if (data.isEmpty()) return null;
        try {
            Path cache = directory.resolve("images");
            Files.createDirectories(cache);
            byte[] bytes = Base64.getDecoder().decode(data);
            String hash = HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            Path file = cache.resolve(hash + ".img");
            synchronized (imageCache) {
                if (!Files.exists(file)) {
                    Path temporary = Files.createTempFile(cache, "image-", ".tmp");
                    try {
                        Files.write(temporary, bytes);
                        Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING);
                    } finally {
                        Files.deleteIfExists(temporary);
                    }
                }
            }
            return file.toString();
        } catch (IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không lưu được ảnh tải từ server.", e);
        }
    }

    @Override
    public List<Item> getSellerItems(String seller) {
        return listProducts(seller).stream().map(Auction::getItem).toList();
    }

    @Override
    public boolean createItemAndAuction(Item item, int minutes, double step) {
        LocalDateTime start = serverNow().plusSeconds(1);
        saveProduct(
                new Auction(
                        UUID.randomUUID().toString(),
                        item,
                        currentUser.getUsername(),
                        item.getStartingPrice(),
                        null,
                        AuctionStatus.OPEN,
                        start,
                        start.plusMinutes(minutes),
                        step),
                null);
        return true;
    }

    @Override
    public boolean updateItem(Item item) {
        Auction a =
                listProducts(currentUser.getUsername()).stream()
                        .filter(v -> v.getItem().getId().equals(item.getId()))
                        .findFirst()
                        .orElseThrow();
        a.setItem(item);
        saveProduct(a, null);
        return true;
    }

    @Override
    public boolean deleteItem(String id) {
        Auction a =
                listProducts(currentUser.getUsername()).stream()
                        .filter(v -> v.getItem().getId().equals(id))
                        .findFirst()
                        .orElseThrow();
        deleteProduct(currentUser.getUsername(), a.getId());
        return true;
    }

    @Override
    public WalletSnapshot getSnapshot(String username) {
        self(username);
        JsonObject o = read("GET_MY_WALLET_SNAPSHOT", obj()).getAsJsonObject();
        List<Transaction> history = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("transactions"))
            history.add(transaction(e.getAsJsonObject()));
        return new WalletSnapshot(n(o, "balance"), n(o, "held"), n(o, "available"), history);
    }

    @Override
    public long getBalance(String u) {
        return getSnapshot(u).balance();
    }

    @Override
    public long getHeldAmount(String u) {
        return getSnapshot(u).held();
    }

    @Override
    public long getAvailable(String u) {
        return getSnapshot(u).available();
    }

    @Override
    public List<Transaction> getTransactions(String u) {
        return getSnapshot(u).transactions();
    }

    @Override
    public void deposit(String u, long amount) {
        self(u);
        write("DEPOSIT_SIMULATED", obj("amount", amount));
    }

    @Override
    public void withdraw(String u, long amount) {
        self(u);
        write("WITHDRAW_SIMULATED", obj("amount", amount));
    }

    @Override
    public AutoBid registerAutoBid(String id, long max, long increment) {
        return auto(
                write(
                                "SET_MY_AUTO_BID",
                                obj("auctionId", id, "maxBid", max, "increment", increment))
                        .getAsJsonObject());
    }

    @Override
    public void cancelAutoBid(String id) {
        write("CANCEL_MY_AUTO_BID", obj("auctionId", id));
    }

    @Override
    public AutoBid getMyAutoBid(String id) {
        JsonElement r = read("GET_MY_AUTO_BID", obj("auctionId", id));
        return r.isJsonNull() ? null : auto(r.getAsJsonObject());
    }

    @Override
    public List<AdminUser> listUsers() {
        List<AdminUser> out = new ArrayList<>();
        for (JsonElement v : read("LIST_USERS", obj()).getAsJsonArray()) {
            JsonObject o = v.getAsJsonObject();
            AdminUser u = new AdminUser(user(o), time(o, "createdAt"));
            u.setLocked(o.get("locked").getAsBoolean());
            out.add(u);
        }
        return out;
    }

    @Override
    public void setLocked(String actor, String id, boolean locked) {
        write(locked ? "LOCK_USER" : "UNLOCK_USER", obj("userId", id));
    }

    @Override
    public void setRole(String actor, String id, UserRole role) {
        write("CHANGE_USER_ROLE", obj("userId", id, "role", role));
    }

    @Override
    public void deleteUser(String actor, String id) {
        write("DELETE_USER", obj("userId", id));
    }

    @Override
    public List<Auction> listAuctions() {
        return auctions(read("ADMIN_LIST_AUCTIONS", obj()));
    }

    @Override
    public void cancelAuction(String id, String reason) {
        write("CANCEL_AUCTION", obj("auctionId", id, "reason", reason));
    }

    @Override
    public void deleteAuction(String id) {
        write("DELETE_AUCTION", obj("auctionId", id));
    }

    @Override
    public void registerBidListener(String id, Consumer<Bid> listener) {
        bidListeners.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unregisterBidListener(String id, Consumer<Bid> listener) {
        var list = bidListeners.get(id);
        if (list != null) list.remove(listener);
    }

    @Override
    public void addChangeListener(Consumer<String> listener) {
        changes.add(listener);
    }

    @Override
    public void removeChangeListener(Consumer<String> listener) {
        changes.remove(listener);
    }

    private void changed(String type) {
        for (var listener : changes)
            try {
                listener.accept(type);
            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
            }
    }

    private void onEvent(JsonObject e) {
        String type = s(e, "event");
        if (type.equals("USER_CHANGED"))
            try {
                currentUser = user(read("GET_ME", obj()).getAsJsonObject());
            } catch (ServerError ignored) {
                return;
            }
        if (type.equals("BID_PLACED") && e.has("bid")) {
            Bid b = bid(e.getAsJsonObject("bid"));
            for (var listener :
                    bidListeners.getOrDefault(b.getAuctionId(), new CopyOnWriteArrayList<>()))
                try {
                    listener.accept(b);
                } catch (RuntimeException error) {
                    // One broken view must not suppress other observers or the change event.
                    System.err.println("Bid listener failed: " + error.getMessage());
                }
        }
        changed(type);
    }

    @Override
    public void close() {
        closed = true;
        heartbeat.shutdownNow();
        transport.close();
        changes.clear();
        bidListeners.clear();
    }
}
