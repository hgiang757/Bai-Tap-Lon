package com.auction.server.service;

import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.dao.AuctionRepository;
import com.auction.server.exception.ApiException;
import com.auction.server.model.*;
import com.google.gson.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Application service: all financial changes use one serializable, durable transaction. A global
 * lock intentionally favors correctness over throughput for a classroom server. This is the one
 * business entry point: authentication, products, bids and wallets are validated and saved in one
 * transaction before events reach the clients.
 */
public final class AuctionService implements AutoCloseable {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final long MAX_MONEY = 9_000_000_000_000L;
    private final AuctionRepository repository;
    private final Clock clock;
    private JsonObject state;
    private final Map<String, Session> sessions = new HashMap<>();
    private final Consumer<JsonObject> publisher;
    private List<JsonObject> events = new ArrayList<>();

    private record Session(String userId, Instant expires) {}

    public AuctionService(Path directory, Clock clock, boolean demo, Consumer<JsonObject> publisher)
            throws IOException {
        this.repository = new AuctionRepository(directory);
        this.clock = clock;
        this.publisher = publisher;
        try {
            state = repository.load();
            if (state == null) {
                state =
                        obj(
                                "schemaVersion",
                                1,
                                "version",
                                0L,
                                "users",
                                new JsonObject(),
                                "auctions",
                                new JsonObject(),
                                "rules",
                                new JsonObject(),
                                "commands",
                                new JsonObject(),
                                "transactions",
                                new JsonArray());
                String adminPassword = System.getenv("AUCTION_ADMIN_PASSWORD");
                if (adminPassword == null || adminPassword.isBlank()) {
                    if (!demo)
                        throw new IOException(
                                "Set AUCTION_ADMIN_PASSWORD (at least 8 characters, letters and"
                                    + " digits)");
                    adminPassword = "Admin12345";
                }
                validatePassword(adminPassword);
                createUser(
                        "admin_hieu",
                        adminPassword,
                        "Quản trị viên",
                        "admin@auction.vn",
                        "ADMIN",
                        0);
                if (demo) seedDemo();
                repository.save(state);
            }
            if (number(state, "schemaVersion") != 1)
                throw new IOException("Unsupported data schema");
        } catch (IOException | RuntimeException e) {
            repository.close();
            throw e;
        }
    }

    public AuctionService(Path directory, boolean demo) throws IOException {
        this(directory, Clock.system(ZONE), demo, ignored -> {});
    }

    private static long number(JsonObject object, String field) {
        try {
            return com.auction.shared.JsonProtocol.number(object, field);
        } catch (IllegalArgumentException e) {
            throw error("VALIDATION_ERROR", "Expected integer: " + field);
        }
    }

    private static ApiException error(String code, String message) {
        return new ApiException(code, message);
    }

    private JsonObject collection(String name) {
        return state.getAsJsonObject(name);
    }

    private List<JsonObject> values(String name) {
        return collection(name).entrySet().stream()
                .map(e -> e.getValue().getAsJsonObject())
                .toList();
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZONE);
    }

    private String id() {
        return UUID.randomUUID().toString();
    }

    private static String required(JsonObject p, String key, int max) {
        String s = text(p, key).strip();
        if (s.isEmpty() || s.length() > max) throw error("VALIDATION_ERROR", "Invalid " + key);
        return s;
    }

    private static long money(JsonObject p, String key) {
        long n = number(p, key);
        if (n <= 0 || n > MAX_MONEY) throw error("VALIDATION_ERROR", "Invalid money: " + key);
        return n;
    }

    private static void validatePassword(String password) {
        if (password.length() < 8
                || password.length() > 128
                || !password.matches("(?s).*[A-Za-z].*")
                || !password.matches("(?s).*[0-9].*"))
            throw error(
                    "VALIDATION_ERROR", "Password requires 8-128 characters, letters and digits");
    }

    private static void requireRole(JsonObject user, String role) {
        if (!text(user, "role").equals(role)) throw error("FORBIDDEN", "Requires " + role);
    }

    private JsonObject authenticatedUser(String token) {
        Session s = sessions.get(token);
        if (s == null || !clock.instant().isBefore(s.expires())) {
            sessions.remove(token);
            throw error("UNAUTHENTICATED", "Session expired");
        }
        JsonObject u = collection("users").getAsJsonObject(s.userId());
        if (u == null || u.get("locked").getAsBoolean() || u.get("deleted").getAsBoolean())
            throw error("UNAUTHENTICATED", "Account unavailable");
        return u;
    }

    public synchronized String authenticatedUserId(String token) {
        return text(authenticatedUser(token), "id");
    }

    private JsonObject publicUser(JsonObject u) {
        JsonObject result = u.deepCopy();
        result.remove("passwordHash");
        return result;
    }

    private JsonObject createUser(
            String username,
            String password,
            String fullName,
            String email,
            String role,
            long balance) {
        String hash = Passwords.hash(password);
        User account =
                switch (role) {
                    case "ADMIN" -> new Admin(id(), username, hash);
                    case "SELLER" -> new Seller(id(), username, hash);
                    case "BIDDER" -> new Bidder(id(), username, hash);
                    default -> throw error("VALIDATION_ERROR", "Unknown role");
                };
        JsonObject u =
                obj(
                        "id",
                        account.getId(),
                        "username",
                        account.getName(),
                        "fullName",
                        fullName,
                        "email",
                        email,
                        "role",
                        account.getRoleName(),
                        "passwordHash",
                        account.getPasswordHash(),
                        "balance",
                        balance,
                        "locked",
                        false,
                        "deleted",
                        false,
                        "createdAt",
                        now());
        collection("users").add(text(u, "id"), u);
        return u;
    }

    private JsonObject login(JsonObject p) {
        String name = required(p, "username", 254);
        JsonObject u =
                values("users").stream()
                        .filter(
                                x ->
                                        text(x, "username").equalsIgnoreCase(name)
                                                || text(x, "email").equalsIgnoreCase(name))
                        .findFirst()
                        .orElse(null);
        if (u == null
                || u.get("locked").getAsBoolean()
                || u.get("deleted").getAsBoolean()
                || !Passwords.verify(text(p, "password"), text(u, "passwordHash")))
            throw error("AUTHENTICATION_FAILED", "Invalid credentials or account locked");
        if (sessions.size() >= 10_000)
            sessions.entrySet().removeIf(e -> !clock.instant().isBefore(e.getValue().expires()));
        if (sessions.size() >= 10_000) throw error("SERVER_BUSY", "Too many sessions");
        String token = id() + id();
        Instant expiry = clock.instant().plus(Duration.ofHours(12));
        sessions.put(token, new Session(text(u, "id"), expiry));
        return obj("token", token, "expiresAt", expiry, "user", publicUser(u));
    }

    private JsonObject register(JsonObject p) {
        String username = required(p, "username", 30),
                email = required(p, "email", 254),
                password = text(p, "password");
        String r = required(p, "role", 10);
        if (!Set.of("BIDDER", "SELLER").contains(r))
            throw error("FORBIDDEN", "Public registration cannot create admin");
        if (!username.matches("[\\p{L}\\p{N}_][\\p{L}\\p{N}_ .-]{1,28}[\\p{L}\\p{N}_]")
                || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw error("VALIDATION_ERROR", "Invalid username or email");
        validatePassword(password);
        if (values("users").stream()
                .anyMatch(
                        u ->
                                text(u, "username").equalsIgnoreCase(username)
                                        || text(u, "email").equalsIgnoreCase(email)))
            throw error("CONFLICT", "Username or email already exists");
        return publicUser(
                createUser(username, password, required(p, "fullName", 120), email, r, 0));
    }

    /** Responses and mutations are serialized; no response exposes mutable application state. */
    // 1. Khóa chung: xác thực -> kiểm tra commandId -> xử lý -> lưu -> phát sự kiện.
    public synchronized JsonObject execute(
            String action, String token, String commandId, JsonObject payload) {
        JsonObject before = state.deepCopy();
        events = new ArrayList<>();
        try {
            refreshAuctions();
            JsonObject user = null;
            if (!Set.of(
                            "PING",
                            "LOGIN",
                            "REGISTER",
                            "LIST_AUCTIONS",
                            "GET_AUCTION",
                            "GET_BID_HISTORY",
                            "GET_IMAGE")
                    .contains(action)) user = authenticatedUser(token);
            boolean write =
                    Set.of(
                                    "REGISTER",
                                    "CREATE_PRODUCT_AUCTION",
                                    "UPDATE_PRODUCT_AUCTION",
                                    "DELETE_PRODUCT_AUCTION",
                                    "PLACE_BID",
                                    "SET_MY_AUTO_BID",
                                    "CANCEL_MY_AUTO_BID",
                                    "DEPOSIT_SIMULATED",
                                    "WITHDRAW_SIMULATED",
                                    "LOCK_USER",
                                    "UNLOCK_USER",
                                    "CHANGE_USER_ROLE",
                                    "DELETE_USER",
                                    "CANCEL_AUCTION",
                                    "DELETE_AUCTION")
                            .contains(action);
            String commandKey = null, fingerprint = action + ":" + payload;
            if (write) {
                if (commandId == null || !commandId.matches("[a-zA-Z0-9_-]{8,100}"))
                    throw error(
                            "VALIDATION_ERROR",
                            "A stable commandId (8-100 safe characters) is required");
                commandKey = (user == null ? "register" : text(user, "id")) + ":" + commandId;
                JsonObject prior = collection("commands").getAsJsonObject(commandKey);
                if (prior != null) {
                    if (!(action.equals("REGISTER")
                            ? Passwords.verify(fingerprint, text(prior, "fingerprint"))
                            : text(prior, "fingerprint").equals(fingerprint)))
                        throw error("CONFLICT", "commandId reused with different payload");
                    return commit(before, prior.getAsJsonObject("response").deepCopy());
                }
            }
            JsonObject response;
            JsonObject afterRefresh = state.deepCopy();
            int refreshEventCount = events.size();
            try {
                response =
                        obj("success", true, "result", handleAction(action, token, user, payload));
            } catch (ApiException e) {
                state = afterRefresh;
                events = new ArrayList<>(events.subList(0, refreshEventCount));
                response = obj("success", false, "errorCode", e.code(), "message", e.getMessage());
            }
            response.addProperty("version", number(state, "version"));
            response.addProperty("serverTime", now().toString());
            if (commandKey != null)
                collection("commands")
                        .add(
                                commandKey,
                                obj(
                                        "fingerprint",
                                        action.equals("REGISTER")
                                                ? Passwords.hash(fingerprint)
                                                : fingerprint,
                                        "status",
                                        response.get("success").getAsBoolean()
                                                ? "SUCCEEDED"
                                                : "REJECTED",
                                        "response",
                                        response.deepCopy()));
            return commit(before, response);
        } catch (ApiException e) {
            return commit(
                    before,
                    obj(
                            "success",
                            false,
                            "errorCode",
                            e.code(),
                            "message",
                            e.getMessage(),
                            "serverTime",
                            now()));
        } catch (RuntimeException e) {
            state = before;
            throw e;
        }
    }

    // Chỉ thông báo cho client sau khi lưu thành công; lỗi lưu thì trả lại bản trước.
    private JsonObject commit(JsonObject before, JsonObject response) {
        try {
            if (!state.equals(before)) repository.save(state);
        } catch (IOException e) {
            state = before;
            events.clear();
            throw error("STORAGE_ERROR", "Unable to commit; operation rolled back");
        }
        for (JsonObject event : events)
            try {
                publisher.accept(event.deepCopy());
            } catch (RuntimeException e) {
                System.err.println(
                        "Event delivery failed; reconcile after reconnect: " + e.getMessage());
            }
        events.clear();
        return response.deepCopy();
    }

    public synchronized void tick() {
        execute("PING", "", "", new JsonObject());
    }

    // 2. Mỗi action được chuyển tới đúng hàm nghiệp vụ, không xử lý Socket ở đây.
    private JsonElement handleAction(String action, String token, JsonObject user, JsonObject p) {
        return switch (action) {
            case "PING" -> obj("status", "OK");
            case "REGISTER" -> register(p);
            case "LOGIN" -> login(p);
            case "LOGOUT" -> {
                sessions.remove(token);
                yield obj("loggedOut", true);
            }
            case "GET_ME" -> publicUser(user);
            case "LIST_AUCTIONS" -> listAuctions(p, null);
            case "LIST_MY_PRODUCTS" -> {
                requireRole(user, "SELLER");
                yield listAuctions(p, text(user, "id"));
            }
            case "GET_AUCTION" -> snapshot(auction(p));
            case "GET_BID_HISTORY" -> auction(p).getAsJsonArray("bids").deepCopy();
            case "GET_ROOM_SNAPSHOT" -> {
                JsonObject a = auction(p);
                yield obj("auction", snapshot(a), "bids", a.getAsJsonArray("bids").deepCopy());
            }
            case "CREATE_PRODUCT_AUCTION" -> createAuction(user, p);
            case "UPDATE_PRODUCT_AUCTION" -> updateAuction(user, p);
            case "DELETE_PRODUCT_AUCTION" -> {
                JsonObject a = auction(p);
                owner(user, a);
                editable(a);
                a.addProperty("deleted", true);
                event("AUCTION_DELETED", a, "");
                yield snapshot(a);
            }
            case "PLACE_BID" -> placeBid(user, p);
            case "SET_MY_AUTO_BID" -> setAuto(user, p);
            case "CANCEL_MY_AUTO_BID" -> {
                JsonObject a = auction(p), rule = rule(user, a);
                if (rule != null) {
                    rule.addProperty("active", false);
                    event("AUTO_BID_CHANGED", a, text(user, "id"));
                }
                yield rule == null ? JsonNull.INSTANCE : rule.deepCopy();
            }
            case "GET_MY_AUTO_BID" -> {
                JsonObject r = rule(user, auction(p));
                yield r == null ? JsonNull.INSTANCE : r.deepCopy();
            }
            case "GET_MY_WALLET_SNAPSHOT" -> {
                JsonObject w = wallet(user);
                w.add("transactions", history(user));
                yield w;
            }
            case "LIST_MY_TRANSACTIONS" -> history(user);
            case "DEPOSIT_SIMULATED", "WITHDRAW_SIMULATED" ->
                    transfer(user, p, action.startsWith("DEPOSIT"));
            case "GET_COMMAND_STATUS" -> {
                JsonObject c =
                        collection("commands")
                                .getAsJsonObject(
                                        text(user, "id") + ":" + required(p, "commandId", 100));
                yield c == null
                        ? obj("status", "NOT_FOUND")
                        : obj(
                                "status",
                                text(c, "status"),
                                "response",
                                c.get("response").deepCopy());
            }
            case "LIST_USERS" -> {
                requireRole(user, "ADMIN");
                JsonArray a = new JsonArray();
                values("users").stream()
                        .filter(u -> !u.get("deleted").getAsBoolean())
                        .forEach(u -> a.add(publicUser(u)));
                yield a;
            }
            case "ADMIN_LIST_AUCTIONS" -> {
                requireRole(user, "ADMIN");
                yield listAuctions(p, null);
            }
            case "LOCK_USER", "UNLOCK_USER", "CHANGE_USER_ROLE", "DELETE_USER" ->
                    manageUser(user, p, action);
            case "CANCEL_AUCTION" -> cancel(user, p);
            case "DELETE_AUCTION" -> {
                requireRole(user, "ADMIN");
                JsonObject a = auction(p);
                if (!text(a, "status").equals("CANCELED"))
                    throw error("CONFLICT", "Only canceled auctions may be archived");
                a.addProperty("deleted", true);
                event("AUCTION_DELETED", a, "");
                yield snapshot(a);
            }
            case "GET_IMAGE" -> {
                JsonObject a = auction(p);
                yield obj("imageBase64", text(a, "imageBase64"), "imageType", text(a, "imageType"));
            }
            default -> throw error("UNKNOWN_ACTION", "Unknown action: " + action);
        };
    }

    private JsonObject auction(JsonObject p) {
        JsonObject a = collection("auctions").getAsJsonObject(required(p, "auctionId", 100));
        if (a == null || a.get("deleted").getAsBoolean())
            throw error("NOT_FOUND", "Auction not found");
        return a;
    }

    private JsonObject snapshot(JsonObject a) {
        JsonObject copy = a.deepCopy();
        copy.remove("bids");
        copy.remove("imageBase64");
        copy.addProperty("hasImage", !text(a, "imageBase64").isEmpty());
        copy.addProperty("minNextBid", number(a, "currentPrice") + number(a, "minBidStep"));
        return copy;
    }

    private JsonArray listAuctions(JsonObject p, String sellerId) {
        JsonArray list = new JsonArray();
        String status = text(p, "status"),
                query = text(p, "search").toLowerCase(Locale.ROOT),
                category = text(p, "category");
        for (JsonObject a : values("auctions"))
            if (!a.get("deleted").getAsBoolean()
                    && (sellerId == null || sellerId.equals(text(a, "sellerId")))
                    && (status.isEmpty() || status.equals(text(a, "status")))
                    && (category.isEmpty() || category.equals(text(a, "category")))
                    && (text(a, "name") + " " + text(a, "sellerUsername"))
                            .toLowerCase(Locale.ROOT)
                            .contains(query)) list.add(snapshot(a));
        return list;
    }

    private void owner(JsonObject user, JsonObject a) {
        requireRole(user, "SELLER");
        if (!text(user, "id").equals(text(a, "sellerId")))
            throw error("FORBIDDEN", "Not your product");
    }

    private void editable(JsonObject a) {
        if (!text(a, "status").equals("OPEN") || !a.getAsJsonArray("bids").isEmpty())
            throw error("CONFLICT", "Only upcoming auctions without bids are editable");
    }

    // 3. Sản phẩm và phiên đấu: kiểm tra danh mục, giá, thời gian, ảnh và quyền sở hữu.
    private JsonObject validateProduct(JsonObject p) {
        String category = required(p, "category", 30);
        if (!Set.of("ELECTRONICS", "ART", "VEHICLE", "JEWELRY", "REAL_ESTATE", "OTHER")
                .contains(category)) throw error("VALIDATION_ERROR", "Unknown category");
        LocalDateTime start, end;
        try {
            start = LocalDateTime.parse(required(p, "startTime", 40));
            end = LocalDateTime.parse(required(p, "endTime", 40));
        } catch (java.time.DateTimeException e) {
            throw error("VALIDATION_ERROR", "Use ISO local time in Asia/Ho_Chi_Minh");
        }
        if (start.isBefore(now().minusSeconds(2))
                || !end.isAfter(start)
                || Duration.between(start, end).compareTo(Duration.ofDays(365)) > 0)
            throw error("VALIDATION_ERROR", "Invalid start/end time (maximum duration 365 days)");
        return obj(
                "name",
                required(p, "name", 200),
                "description",
                required(p, "description", 10_000),
                "category",
                category,
                "startingPrice",
                money(p, "startingPrice"),
                "minBidStep",
                money(p, "minBidStep"),
                "startTime",
                start,
                "endTime",
                end);
    }

    private void image(JsonObject a, JsonObject p) {
        if (!p.has("imageBase64")) return;
        String encoded = text(p, "imageBase64");
        if (!encoded.isEmpty()) {
            byte[] data;
            try {
                data = Base64.getDecoder().decode(encoded);
            } catch (IllegalArgumentException e) {
                throw error("VALIDATION_ERROR", "Invalid image encoding");
            }
            if (data.length >= 5 * 1024 * 1024)
                throw error("VALIDATION_ERROR", "Image must be smaller than 5 MB");
            try (var in =
                    javax.imageio.ImageIO.createImageInputStream(
                            new java.io.ByteArrayInputStream(data))) {
                var readers = javax.imageio.ImageIO.getImageReaders(in);
                if (!readers.hasNext()) throw error("VALIDATION_ERROR", "Unsupported image");
                var reader = readers.next();
                try {
                    reader.setInput(in);
                    if ((long) reader.getWidth(0) * reader.getHeight(0) > 40_000_000)
                        throw error("VALIDATION_ERROR", "Image dimensions too large");
                    a.addProperty("imageType", reader.getFormatName());
                } finally {
                    reader.dispose();
                }
            } catch (IOException e) {
                throw error("VALIDATION_ERROR", "Invalid image");
            }
        }
        a.addProperty("imageBase64", encoded);
    }

    private JsonObject createAuction(JsonObject user, JsonObject p) {
        requireRole(user, "SELLER");
        JsonObject a = validateProduct(p);
        String key = id();
        var item =
                ItemFactory.forCategory(text(a, "category"))
                        .create(
                                id(),
                                text(a, "name"),
                                text(a, "description"),
                                new Seller(text(user, "id"), text(user, "username"), ""));
        a.addProperty("category", item.getCategory());
        a.addProperty("id", key);
        a.addProperty("itemId", item.getId());
        a.addProperty("sellerId", text(user, "id"));
        a.addProperty("sellerUsername", text(user, "username"));
        a.addProperty("currentPrice", number(a, "startingPrice"));
        a.addProperty(
                "status",
                LocalDateTime.parse(text(a, "startTime")).isAfter(now()) ? "OPEN" : "RUNNING");
        a.addProperty("highestBidderId", "");
        a.addProperty("highestBidderUsername", "");
        a.addProperty("totalBids", 0);
        a.addProperty("deleted", false);
        a.addProperty("settled", false);
        a.addProperty("imageBase64", "");
        a.addProperty("imageType", "");
        a.add("bids", new JsonArray());
        image(a, p);
        collection("auctions").add(key, a);
        event("AUCTION_CREATED", a, "");
        return snapshot(a);
    }

    private JsonObject updateAuction(JsonObject user, JsonObject p) {
        JsonObject a = auction(p);
        owner(user, a);
        editable(a);
        JsonObject fields = validateProduct(p);
        fields.entrySet().forEach(e -> a.add(e.getKey(), e.getValue()));
        a.addProperty("currentPrice", number(a, "startingPrice"));
        image(a, p);
        event("AUCTION_UPDATED", a, "");
        return snapshot(a);
    }

    // 4. Tiền khả dụng = tổng số dư - tiền đang giữ ở những phiên đang dẫn đầu.
    private JsonObject wallet(JsonObject user) {
        long total = number(user, "balance"), held = heldBalance(text(user, "id"));
        return obj(
                "balance",
                total,
                "held",
                held,
                "available",
                total - held,
                "version",
                number(state, "version"));
    }

    private long heldBalance(String userId) {
        return values("auctions").stream()
                .filter(
                        a ->
                                !a.get("settled").getAsBoolean()
                                        && text(a, "highestBidderId").equals(userId))
                .mapToLong(a -> number(a, "currentPrice"))
                .sum();
    }

    private long availableForAuction(JsonObject u, JsonObject a) {
        return number(u, "balance")
                - heldBalance(text(u, "id"))
                + (text(a, "highestBidderId").equals(text(u, "id"))
                        ? number(a, "currentPrice")
                        : 0);
    }

    private void log(JsonObject user, String type, long amount, String auctionId) {
        state.getAsJsonArray("transactions")
                .add(
                        obj(
                                "id",
                                id(),
                                "username",
                                text(user, "username"),
                                "userId",
                                text(user, "id"),
                                "type",
                                type,
                                "amount",
                                amount,
                                "balanceAfter",
                                number(user, "balance"),
                                "timestamp",
                                now(),
                                "status",
                                "Thành công",
                                "note",
                                "",
                                "auctionId",
                                auctionId));
        event("WALLET_CHANGED", null, text(user, "id"));
    }

    private JsonArray history(JsonObject u) {
        JsonArray out = new JsonArray();
        JsonArray all = state.getAsJsonArray("transactions");
        for (int i = all.size() - 1; i >= 0; i--)
            if (text(all.get(i).getAsJsonObject(), "userId").equals(text(u, "id")))
                out.add(all.get(i).deepCopy());
        return out;
    }

    private JsonObject transfer(JsonObject user, JsonObject p, boolean deposit) {
        if (text(user, "role").equals("ADMIN"))
            throw error("FORBIDDEN", "Admin cannot use participant wallet");
        long amount = money(p, "amount"), balance = number(user, "balance");
        if (deposit && amount < 10_000)
            throw error("VALIDATION_ERROR", "Minimum deposit is 10000 VND");
        if (!deposit && amount > balance - heldBalance(text(user, "id")))
            throw error("INSUFFICIENT_FUNDS", "Insufficient available funds");
        if (deposit && balance > MAX_MONEY - amount)
            throw error("VALIDATION_ERROR", "Wallet limit exceeded");
        user.addProperty("balance", deposit ? balance + amount : balance - amount);
        log(user, deposit ? "DEPOSIT" : "WITHDRAW", amount, "");
        return wallet(user);
    }

    private void validateBidder(JsonObject user, JsonObject a) {
        if (text(user, "role").equals("ADMIN") || text(user, "id").equals(text(a, "sellerId")))
            throw error("FORBIDDEN", "Cannot bid on this product");
        if (LocalDateTime.parse(text(a, "startTime")).isAfter(now()))
            throw error("AUCTION_NOT_STARTED", "Auction has not started");
        if (!text(a, "status").equals("RUNNING")
                || !now().isBefore(LocalDateTime.parse(text(a, "endTime"))))
            throw error("AUCTION_CLOSED", "Auction is closed");
    }

    // 5. Đặt giá: kiểm tra phiên/quyền/giá/tiền, chấp nhận bid rồi chạy auto-bid.
    private JsonObject placeBid(JsonObject user, JsonObject p) {
        JsonObject a = auction(p);
        validateBidder(user, a);
        long amount = money(p, "amount");
        if (amount < number(a, "currentPrice") + number(a, "minBidStep"))
            throw error("BID_TOO_LOW", "Bid must reach current price plus minimum step");
        if (availableForAuction(user, a) < amount)
            throw error("INSUFFICIENT_FUNDS", "Insufficient available funds");
        acceptBid(a, user, amount, false);
        processAutoBids(a);
        return obj("auction", snapshot(a), "bids", a.getAsJsonArray("bids").deepCopy());
    }

    private void acceptBid(JsonObject a, JsonObject user, long amount, boolean automatic) {
        String previous = text(a, "highestBidderId");
        long previousAmount = number(a, "currentPrice");
        a.addProperty("highestBidderId", text(user, "id"));
        a.addProperty("highestBidderUsername", text(user, "username"));
        a.addProperty("currentPrice", amount);
        if (!previous.isEmpty())
            log(
                    collection("users").getAsJsonObject(previous),
                    "BID_RELEASE",
                    previousAmount,
                    text(a, "id"));
        log(user, "BID_HOLD", amount, text(a, "id"));
        JsonObject bid =
                obj(
                        "id",
                        id(),
                        "auctionId",
                        text(a, "id"),
                        "bidderUsername",
                        text(user, "username"),
                        "amount",
                        amount,
                        "timestamp",
                        now(),
                        "auto",
                        automatic);
        a.getAsJsonArray("bids").add(bid);
        a.addProperty("totalBids", a.getAsJsonArray("bids").size());
        LocalDateTime end = LocalDateTime.parse(text(a, "endTime"));
        LocalDateTime extended = new AntiSnipingPolicy().computeNewEndTime(end, now());
        if (!end.equals(extended)) {
            a.addProperty("endTime", extended.toString());
            event("AUCTION_EXTENDED", a, "");
        }
        event("BID_PLACED", a, "");
    }

    private String ruleKey(JsonObject user, JsonObject a) {
        return text(a, "id") + ":" + text(user, "id");
    }

    private JsonObject rule(JsonObject u, JsonObject a) {
        return collection("rules").getAsJsonObject(ruleKey(u, a));
    }

    private JsonObject setAuto(JsonObject user, JsonObject p) {
        JsonObject a = auction(p);
        validateBidder(user, a);
        long max = money(p, "maxBid"), increment = money(p, "increment");
        if (increment < number(a, "minBidStep")
                || max < number(a, "currentPrice") + number(a, "minBidStep"))
            throw error("VALIDATION_ERROR", "Invalid auto-bid step or maximum");
        if (availableForAuction(user, a) < max)
            throw error("INSUFFICIENT_FUNDS", "Maximum exceeds available budget");
        JsonObject r =
                obj(
                        "id",
                        id(),
                        "auctionId",
                        text(a, "id"),
                        "userId",
                        text(user, "id"),
                        "username",
                        text(user, "username"),
                        "maxBid",
                        max,
                        "increment",
                        increment,
                        "createdAt",
                        now(),
                        "order",
                        number(state, "version") + 1,
                        "active",
                        true,
                        "limitReached",
                        false,
                        "autoCount",
                        0);
        collection("rules").add(ruleKey(user, a), r);
        event("AUTO_BID_CHANGED", a, text(user, "id"));
        processAutoBids(a);
        return r.deepCopy();
    }

    // Duyệt luật theo thứ tự đăng ký; mỗi lượt chọn một bid hợp lệ và lặp đến khi dừng.
    private void processAutoBids(JsonObject a) {
        List<JsonObject> rules =
                values("rules").stream()
                        .filter(r -> text(r, "auctionId").equals(text(a, "id")))
                        .sorted(Comparator.comparingLong(r -> number(r, "order")))
                        .toList();
        for (int round = 0; round < 10_000; round++) {
            boolean placed = false;
            for (JsonObject r : rules) {
                if (!r.get("active").getAsBoolean()
                        || text(r, "userId").equals(text(a, "highestBidderId"))) continue;
                JsonObject user = collection("users").getAsJsonObject(text(r, "userId"));
                long minimum = number(a, "currentPrice") + number(a, "minBidStep"),
                        max = number(r, "maxBid");
                if (minimum > max) {
                    r.addProperty("active", false);
                    r.addProperty("limitReached", true);
                    continue;
                }
                if (user.get("locked").getAsBoolean()
                        || user.get("deleted").getAsBoolean()
                        || text(user, "role").equals("ADMIN")) {
                    r.addProperty("active", false);
                    continue;
                }
                long price = Math.min(max, number(a, "currentPrice") + number(r, "increment"));
                if (price == max
                        && rules.stream()
                                .anyMatch(
                                        other ->
                                                other != r
                                                        && other.get("active").getAsBoolean()
                                                        && number(other, "maxBid") == max
                                                        && number(other, "order")
                                                                < number(r, "order"))) {
                    r.addProperty("active", false);
                    r.addProperty("limitReached", true);
                    continue;
                }
                if (availableForAuction(user, a) < price) continue;
                acceptBid(a, user, price, true);
                r.addProperty("autoCount", number(r, "autoCount") + 1);
                if (price == max) {
                    r.addProperty("active", false);
                    r.addProperty("limitReached", true);
                }
                placed = true;
                break;
            }
            if (!placed) return;
        }
        // Roll back rather than commit a partially evaluated automatic competition.
        throw error("AUTO_BID_LIMIT", "Too many auto-bid rounds; increase the increment");
    }

    // 6. Đồng hồ server quyết định mở/kết thúc phiên; hết giờ thì thanh toán một lần.
    private void refreshAuctions() {
        for (JsonObject a : values("auctions")) {
            String status = text(a, "status");
            if (a.get("deleted").getAsBoolean() || !Set.of("OPEN", "RUNNING").contains(status))
                continue;
            if (!now().isBefore(LocalDateTime.parse(text(a, "endTime")))) {
                settle(a);
                continue;
            }
            if (status.equals("OPEN")
                    && !now().isBefore(LocalDateTime.parse(text(a, "startTime")))) {
                a.addProperty("status", "RUNNING");
                event("AUCTION_STARTED", a, "");
            }
        }
    }

    private void stopRules(JsonObject a) {
        values("rules").stream()
                .filter(r -> text(r, "auctionId").equals(text(a, "id")))
                .forEach(r -> r.addProperty("active", false));
    }

    private void settle(JsonObject a) {
        a.addProperty("status", "FINISHED");
        stopRules(a);
        String winner = text(a, "highestBidderId");
        if (!winner.isEmpty() && !a.get("settled").getAsBoolean()) {
            JsonObject buyer = collection("users").getAsJsonObject(winner),
                    seller = collection("users").getAsJsonObject(text(a, "sellerId"));
            long amount = number(a, "currentPrice");
            buyer.addProperty("balance", number(buyer, "balance") - amount);
            seller.addProperty("balance", Math.addExact(number(seller, "balance"), amount));
            a.addProperty("status", "PAID");
            a.addProperty("paidAt", now().toString());
            log(buyer, "PAYMENT", amount, text(a, "id"));
            log(seller, "SALE", amount, text(a, "id"));
        }
        a.addProperty("settled", true);
        event("AUCTION_FINISHED", a, "");
    }

    private JsonObject cancel(JsonObject user, JsonObject p) {
        requireRole(user, "ADMIN");
        JsonObject a = auction(p);
        String reason = required(p, "reason", 1000);
        if (!Set.of("OPEN", "RUNNING").contains(text(a, "status")))
            throw error("CONFLICT", "Auction cannot be canceled");
        a.addProperty("status", "CANCELED");
        a.addProperty("cancellationReason", reason);
        a.addProperty("settled", true);
        stopRules(a);
        if (!text(a, "highestBidderId").isEmpty())
            log(
                    collection("users").getAsJsonObject(text(a, "highestBidderId")),
                    "BID_RELEASE",
                    number(a, "currentPrice"),
                    text(a, "id"));
        event("AUCTION_CANCELED", a, "");
        return snapshot(a);
    }

    private JsonObject manageUser(JsonObject actor, JsonObject p, String action) {
        requireRole(actor, "ADMIN");
        String target = required(p, "userId", 100);
        JsonObject u = collection("users").getAsJsonObject(target);
        if (u == null || u.get("deleted").getAsBoolean())
            throw error("NOT_FOUND", "User not found");
        if (target.equals(text(actor, "id")))
            throw error("FORBIDDEN", "Cannot modify your own admin account");
        if ((action.equals("DELETE_USER") || action.equals("CHANGE_USER_ROLE"))
                && (heldBalance(target) > 0
                        || values("auctions").stream()
                                .anyMatch(
                                        a ->
                                                text(a, "sellerId").equals(target)
                                                        && Set.of("OPEN", "RUNNING")
                                                                .contains(text(a, "status")))))
            throw error("CONFLICT", "Account has active auction obligations");
        switch (action) {
            case "LOCK_USER" -> u.addProperty("locked", true);
            case "UNLOCK_USER" -> u.addProperty("locked", false);
            case "DELETE_USER" -> u.addProperty("deleted", true);
            case "CHANGE_USER_ROLE" -> {
                String r = required(p, "role", 10);
                if (!Set.of("ADMIN", "BIDDER", "SELLER").contains(r))
                    throw error("VALIDATION_ERROR", "Unknown role");
                u.addProperty("role", r);
            }
            default -> throw error("UNKNOWN_ACTION", action);
        }
        if (!action.equals("UNLOCK_USER"))
            values("rules").stream()
                    .filter(r -> text(r, "userId").equals(target))
                    .forEach(r -> r.addProperty("active", false));
        event("USER_CHANGED", null, target);
        return publicUser(u);
    }

    private void event(String type, JsonObject auction, String userId) {
        long version = number(state, "version") + 1;
        state.addProperty("version", version);
        if (auction != null) auction.addProperty("version", version);
        JsonObject emitted =
                obj(
                        "type",
                        "event",
                        "event",
                        type,
                        "eventId",
                        id(),
                        "version",
                        version,
                        "serverTime",
                        now(),
                        "auctionId",
                        auction == null ? "" : text(auction, "id"),
                        "userId",
                        userId,
                        "auction",
                        auction == null ? JsonNull.INSTANCE : snapshot(auction));
        if (type.equals("BID_PLACED")) {
            JsonArray bids = auction.getAsJsonArray("bids");
            emitted.add("bid", bids.get(bids.size() - 1).deepCopy());
        }
        events.add(emitted);
    }

    private void seedDemo() {
        JsonObject bidder =
                createUser(
                        "bidder_hieu",
                        "Bidder12345",
                        "Người đấu giá",
                        "hieu@auction.vn",
                        "BIDDER",
                        100_000_000);
        createUser(
                "bidder_vip99",
                "Bidder12345",
                "Người đấu giá 2",
                "vip@auction.vn",
                "BIDDER",
                300_000_000);
        JsonObject seller =
                createUser(
                        "seller_hieu",
                        "Seller12345",
                        "Người bán",
                        "seller@auction.vn",
                        "SELLER",
                        0);
        for (String category :
                List.of("ELECTRONICS", "ART", "VEHICLE", "JEWELRY", "REAL_ESTATE", "OTHER")) {
            createAuction(
                    seller,
                    obj(
                            "name",
                            "Sản phẩm " + category,
                            "description",
                            "Sản phẩm demo để kiểm thử tích hợp",
                            "category",
                            category,
                            "startingPrice",
                            1_000_000,
                            "minBidStep",
                            100_000,
                            "startTime",
                            now(),
                            "endTime",
                            now().plusHours(2)));
        }
        log(bidder, "DEPOSIT", 100_000_000, "");
        events.clear();
    }

    @Override
    public synchronized void close() throws IOException {
        repository.close();
    }
}
