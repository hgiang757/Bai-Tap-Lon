package com.auction.server.service;

import static com.auction.server.service.AuctionContext.*;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.dao.AuctionRepository;
import com.auction.server.exception.ApiException;
import com.google.gson.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Transaction facade. One global monitor covers every wallet, auction, automatic bid and receipt.
 * Business helpers share one context; only this facade commits, rolls back and publishes.
 */
public final class AuctionService implements AutoCloseable {
    public static final ZoneId ZONE = AuctionContext.ZONE;
    public static final long MAX_MONEY = AuctionContext.MAX_MONEY;
    private final AuctionRepository repository;
    private final AuctionContext context;
    private final AuthLogic auth;
    private final ProductLogic products;
    private final WalletLogic wallets;
    private final BiddingLogic bidding;
    private final SettlementLogic settlement;
    private final Consumer<JsonObject> publisher;

    public AuctionService(Path directory, Clock clock, boolean demo, Consumer<JsonObject> publisher)
            throws IOException {
        this.repository = new AuctionRepository(directory);
        this.context = new AuctionContext(clock);
        this.wallets = new WalletLogic(context);
        this.auth = new AuthLogic(context, wallets);
        this.products = new ProductLogic(context);
        this.bidding = new BiddingLogic(context, wallets);
        this.settlement = new SettlementLogic(context, wallets);
        this.publisher = publisher;
        try {
            context.state = repository.load();
            if (context.state == null) {
                context.state =
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
                AuthLogic.validatePassword(adminPassword);
                auth.createUser(
                        "admin_hieu",
                        adminPassword,
                        "Quản trị viên",
                        "admin@auction.vn",
                        "ADMIN",
                        0);
                if (demo) seedDemo();
                repository.save(context.state);
            }
            AuctionStateValidator.validate(context.state);
            Passwords.warmUp();
        } catch (IOException | RuntimeException e) {
            repository.close();
            throw e;
        }
    }

    public AuctionService(Path directory, boolean demo) throws IOException {
        this(directory, Clock.system(ZONE), demo, ignored -> {});
    }

    public synchronized String authenticatedUserId(String token) {
        return text(auth.authenticatedUser(token), "id");
    }

    // Khóa chung: xác thực -> commandId -> nghiệp vụ -> lưu -> phát sự kiện.
    public synchronized JsonObject execute(
            String action, String token, String commandId, JsonObject payload) {
        JsonObject before = context.state.deepCopy();
        context.events = new ArrayList<>();
        try {
            settlement.refreshAuctions();
            JsonObject user = null;
            if (!Set.of(
                            "PING",
                            "LOGIN",
                            "REGISTER",
                            "LIST_AUCTIONS",
                            "GET_AUCTION",
                            "GET_BID_HISTORY",
                            "GET_IMAGE")
                    .contains(action)) user = auth.authenticatedUser(token);
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
                JsonObject prior = context.collection("commands").getAsJsonObject(commandKey);
                if (prior != null) {
                    if (!(action.equals("REGISTER")
                            ? Passwords.verify(fingerprint, text(prior, "fingerprint"))
                            : text(prior, "fingerprint").equals(fingerprint)))
                        throw error("CONFLICT", "commandId reused with different payload");
                    return commit(before, prior.getAsJsonObject("response").deepCopy());
                }
            }
            JsonObject response;
            JsonObject afterRefresh = context.state.deepCopy();
            int refreshEventCount = context.events.size();
            try {
                response =
                        obj("success", true, "result", handleAction(action, token, user, payload));
            } catch (ApiException e) {
                context.state = afterRefresh;
                context.events = new ArrayList<>(context.events.subList(0, refreshEventCount));
                response = obj("success", false, "errorCode", e.code(), "message", e.getMessage());
            }
            response.addProperty("version", number(context.state, "version"));
            response.addProperty("serverTime", context.now().toString());
            if (commandKey != null)
                context.collection("commands")
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
                            context.now()));
        } catch (RuntimeException e) {
            context.state = before;
            throw e;
        }
    }

    // Chỉ thông báo sau khi lưu thành công; lỗi lưu khôi phục toàn bộ snapshot.
    private JsonObject commit(JsonObject before, JsonObject response) {
        try {
            if (!context.state.equals(before)) repository.save(context.state);
        } catch (IOException e) {
            context.state = before;
            context.events.clear();
            System.err.println("State commit failed: " + e.getMessage());
            throw error("STORAGE_ERROR", "Unable to commit; operation rolled back");
        }
        for (JsonObject event : context.events)
            try {
                publisher.accept(event.deepCopy());
            } catch (RuntimeException e) {
                System.err.println(
                        "Event delivery failed; reconcile after reconnect: " + e.getMessage());
            }
        context.events.clear();
        return response.deepCopy();
    }

    public synchronized void tick() {
        execute("PING", "", "", new JsonObject());
    }

    // Action dispatcher: each helper runs under this service's existing monitor.
    private JsonElement handleAction(String action, String token, JsonObject user, JsonObject p) {
        return switch (action) {
            case "PING" -> obj("status", "OK");
            case "REGISTER" -> auth.register(p);
            case "LOGIN" -> auth.login(p);
            case "LOGOUT" -> {
                auth.logout(token);
                yield obj("loggedOut", true);
            }
            case "GET_ME" -> auth.publicUser(user);
            case "LIST_AUCTIONS" -> products.listAuctions(p, null);
            case "LIST_MY_PRODUCTS" -> {
                requireRole(user, "SELLER");
                yield products.listAuctions(p, text(user, "id"));
            }
            case "GET_AUCTION" -> context.snapshot(context.auction(p));
            case "GET_BID_HISTORY" -> context.auction(p).getAsJsonArray("bids").deepCopy();
            case "GET_ROOM_SNAPSHOT" -> {
                JsonObject a = context.auction(p);
                yield obj("auction", context.snapshot(a), "bids", a.getAsJsonArray("bids").deepCopy());
            }
            case "CREATE_PRODUCT_AUCTION" -> products.createAuction(user, p);
            case "UPDATE_PRODUCT_AUCTION" -> products.updateAuction(user, p);
            case "DELETE_PRODUCT_AUCTION" -> {
                JsonObject a = context.auction(p);
                products.owner(user, a);
                products.editable(a);
                a.addProperty("deleted", true);
                context.event("AUCTION_DELETED", a, "");
                yield context.snapshot(a);
            }
            case "PLACE_BID" -> bidding.placeBid(user, p);
            case "SET_MY_AUTO_BID" -> bidding.setAuto(user, p);
            case "CANCEL_MY_AUTO_BID" -> {
                JsonObject a = context.auction(p), rule = bidding.rule(user, a);
                if (rule != null) {
                    rule.addProperty("active", false);
                    context.event("AUTO_BID_CHANGED", a, text(user, "id"));
                }
                yield rule == null ? JsonNull.INSTANCE : rule.deepCopy();
            }
            case "GET_MY_AUTO_BID" -> {
                JsonObject r = bidding.rule(user, context.auction(p));
                yield r == null ? JsonNull.INSTANCE : r.deepCopy();
            }
            case "GET_MY_WALLET_SNAPSHOT" -> {
                JsonObject w = wallets.wallet(user);
                w.add("transactions", wallets.history(user));
                yield w;
            }
            case "LIST_MY_TRANSACTIONS" -> wallets.history(user);
            case "DEPOSIT_SIMULATED", "WITHDRAW_SIMULATED" ->
                    wallets.transfer(user, p, action.startsWith("DEPOSIT"));
            case "GET_COMMAND_STATUS" -> {
                JsonObject c =
                        context.collection("commands")
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
                context.values("users").stream()
                        .filter(u -> !u.get("deleted").getAsBoolean())
                        .forEach(u -> a.add(auth.publicUser(u)));
                yield a;
            }
            case "ADMIN_LIST_AUCTIONS" -> {
                requireRole(user, "ADMIN");
                yield products.listAuctions(p, null);
            }
            case "LOCK_USER", "UNLOCK_USER", "CHANGE_USER_ROLE", "DELETE_USER" ->
                    auth.manageUser(user, p, action);
            case "CANCEL_AUCTION" -> settlement.cancel(user, p);
            case "DELETE_AUCTION" -> {
                requireRole(user, "ADMIN");
                JsonObject a = context.auction(p);
                if (!text(a, "status").equals("CANCELED"))
                    throw error("CONFLICT", "Only canceled auctions may be archived");
                a.addProperty("deleted", true);
                context.event("AUCTION_DELETED", a, "");
                yield context.snapshot(a);
            }
            case "GET_IMAGE" -> {
                JsonObject a = context.auction(p);
                yield obj("imageBase64", text(a, "imageBase64"), "imageType", text(a, "imageType"));
            }
            default -> throw error("UNKNOWN_ACTION", "Unknown action: " + action);
        };
    }

    private void seedDemo() {
        JsonObject bidder =
                auth.createUser(
                        "bidder_hieu",
                        "Bidder12345",
                        "Người đấu giá",
                        "hieu@auction.vn",
                        "BIDDER",
                        100_000_000);
        auth.createUser(
                "bidder_vip99",
                "Bidder12345",
                "Người đấu giá 2",
                "vip@auction.vn",
                "BIDDER",
                300_000_000);
        JsonObject seller =
                auth.createUser(
                        "seller_hieu",
                        "Seller12345",
                        "Người bán",
                        "seller@auction.vn",
                        "SELLER",
                        0);
        for (String category :
                List.of("ELECTRONICS", "ART", "VEHICLE", "JEWELRY", "REAL_ESTATE", "OTHER")) {
            products.createAuction(
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
                            context.now(),
                            "endTime",
                            context.now().plusHours(2)));
        }
        wallets.log(bidder, "DEPOSIT", 100_000_000, "");
        context.events.clear();
    }

    @Override
    public synchronized void close() throws IOException {
        repository.close();
    }
}
