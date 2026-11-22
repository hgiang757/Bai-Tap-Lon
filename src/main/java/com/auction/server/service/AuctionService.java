package com.auction.server.service;

import static com.auction.server.service.AuctionContext.error;
import static com.auction.server.service.AuctionContext.number;
import static com.auction.server.service.AuctionContext.requireRole;
import static com.auction.server.service.AuctionContext.required;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.assistant.AuctionProduct;
import com.auction.server.dao.AuctionRepository;
import com.auction.server.exception.ApiException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Một khóa chung bảo vệ giao dịch tác động đến ví, phiên, giá tự động và biên nhận lệnh.
 * Các lớp nghiệp vụ dùng chung trạng thái. Lớp này lưu, hoàn tác và phát sự kiện.
 */
public final class AuctionService implements AutoCloseable {
  public static final ZoneId ZONE = AuctionContext.ZONE;
  public static final long MAX_MONEY = AuctionContext.MAX_MONEY;
  private static final Set<String> ACTIONS_WITHOUT_SESSION =
      Set.of(
          "PING",
          "LOGIN",
          "REGISTER",
          "LIST_AUCTIONS",
          "GET_AUCTION",
          "GET_BID_HISTORY",
          "GET_IMAGE");
  private static final Set<String> ACTIONS_THAT_CHANGE_STATE =
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
          "DELETE_AUCTION");
  private final AuctionRepository repository;
  private final AuctionContext context;
  private final AuthLogic auth;
  private final ProductLogic products;
  private final WalletLogic wallets;
  private final BiddingLogic bidding;
  private final SettlementLogic settlement;
  private final Consumer<JsonObject> publisher;

  /** Nạp hoặc khởi tạo trạng thái đấu giá cùng các thành phần xử lý giao dịch. */
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
          if (!demo) {
            throw new IOException(
                "Set AUCTION_ADMIN_PASSWORD (at least 8 characters, letters and" + " digits)");
          }
          adminPassword = "Admin12345";
        }
        AuthLogic.validatePassword(adminPassword);
        auth.createUser(
            "admin_hieu", adminPassword, "Quản trị viên", "admin@auction.vn", "ADMIN", 0);
        if (demo) {
          seedDemo();
        }
        repository.save(context.state);
      }
      AuctionStateValidator.validate(context.state);
      Passwords.warmUp();
    } catch (IOException | RuntimeException e) {
      repository.close();
      throw e;
    }
  }

  /** Nạp hoặc khởi tạo trạng thái đấu giá cùng các thành phần xử lý giao dịch. */
  public AuctionService(Path directory, boolean demo) throws IOException {
    this(directory, Clock.system(ZONE), demo, ignored -> {});
  }

  /** Xác thực phiên đăng nhập và trả về mã tài khoản. */
  public synchronized String authenticatedUserId(String token) {
    return text(auth.authenticatedUser(token), "id");
  }

  /** Mã tài khoản và thông tin được phép đọc trong phòng đấu giá. */
  public record AssistantContext(String userId, AuctionProduct product) {}

  /** Đọc giá hiện tại dưới khóa giao dịch và trả về bản sao dữ liệu cần thiết. */
  public synchronized AssistantContext assistantContext(String token, JsonObject payload) {
    String userId = authenticatedUserId(token);
    // Giống GET_ROOM_SNAPSHOT, người đã đăng nhập được đọc phòng chưa lưu trữ.
    JsonObject auction = context.auction(payload);
    return new AssistantContext(
        userId,
        new AuctionProduct(
            text(auction, "id"),
            text(auction, "itemId"),
            text(auction, "name"),
            text(auction, "category"),
            text(auction, "description"),
            text(auction, "condition"),
            text(auction, "status"),
            number(auction, "startingPrice"),
            number(auction, "currentPrice"),
            number(auction, "minBidStep"),
            number(auction, "totalBids") > 0));
  }

  // Khóa chung: xác thực -> commandId -> nghiệp vụ -> lưu -> phát sự kiện.
  /** Xử lý yêu cầu dưới khóa giao dịch chung rồi lưu hoặc hoàn tác giao dịch. */
  public synchronized JsonObject execute(
      String action, String token, String commandId, JsonObject payload) {
    JsonObject before = context.state.deepCopy();
    context.events = new ArrayList<>();
    try {
      settlement.refreshAuctions();
      JsonObject user = null;
      if (!ACTIONS_WITHOUT_SESSION.contains(action)) {
        user = auth.authenticatedUser(token);
      }
      boolean changesState = ACTIONS_THAT_CHANGE_STATE.contains(action);
      String fingerprint = action + ":" + payload;
      String commandKey = changesState ? createCommandKey(user, commandId) : null;
      if (commandKey != null) {
        JsonObject previousResponse = previousCommandResponse(commandKey, action, fingerprint);
        if (previousResponse != null) {
          return commit(before, previousResponse);
        }
      }
      JsonObject response = dispatchAction(action, token, user, payload);
      response.addProperty("version", number(context.state, "version"));
      response.addProperty("serverTime", context.now().toString());
      if (commandKey != null) {
        rememberCommand(commandKey, action, fingerprint, response);
      }
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

  /** Tạo khóa định danh yêu cầu để nhận biết lần gửi lại. */
  private String createCommandKey(JsonObject user, String commandId) {
    if (commandId == null || !commandId.matches("[a-zA-Z0-9_-]{8,100}")) {
      throw error("VALIDATION_ERROR", "A stable commandId (8-100 safe characters) is required");
    }
    return (user == null ? "register" : text(user, "id")) + ":" + commandId;
  }

  /** Trả phản hồi cũ nếu đây là lần gửi lại hợp lệ; trả null nếu lệnh chưa từng chạy. */
  private JsonObject previousCommandResponse(String key, String action, String fingerprint) {
    JsonObject previous = context.collection("commands").getAsJsonObject(key);
    if (previous == null) {
      return null;
    }
    boolean sameRequest =
        action.equals("REGISTER")
            ? Passwords.verify(fingerprint, text(previous, "fingerprint"))
            : text(previous, "fingerprint").equals(fingerprint);
    if (!sameRequest) {
      throw error("CONFLICT", "commandId reused with different payload");
    }
    return previous.getAsJsonObject("response").deepCopy();
  }

  /** Lưu phản hồi để lần gửi lại không thực hiện nghiệp vụ lần thứ hai. */
  private void rememberCommand(
      String key, String action, String fingerprint, JsonObject response) {
    context
        .collection("commands")
        .add(
            key,
            obj(
                "fingerprint",
                action.equals("REGISTER") ? Passwords.hash(fingerprint) : fingerprint,
                "status",
                response.get("success").getAsBoolean() ? "SUCCEEDED" : "REJECTED",
                "response",
                response.deepCopy()));
  }

  /** Chạy nghiệp vụ; nếu bị từ chối thì bỏ các thay đổi phát sinh riêng từ yêu cầu này. */
  private JsonObject dispatchAction(
      String action, String token, JsonObject user, JsonObject payload) {
    JsonObject stateAfterRefresh = context.state.deepCopy();
    int refreshEventCount = context.events.size();
    try {
      return obj("success", true, "result", handleAction(action, token, user, payload));
    } catch (ApiException e) {
      context.state = stateAfterRefresh;
      context.events = new ArrayList<>(context.events.subList(0, refreshEventCount));
      return obj("success", false, "errorCode", e.code(), "message", e.getMessage());
    }
  }

  // Chỉ thông báo sau khi lưu thành công; khi lưu lỗi, khôi phục toàn bộ bản chụp trạng thái.
  private JsonObject commit(JsonObject before, JsonObject response) {
    try {
      if (!context.state.equals(before)) {
        repository.save(context.state);
      }
    } catch (IOException e) {
      context.state = before;
      context.events.clear();
      System.err.println("State commit failed: " + e.getMessage());
      throw error("STORAGE_ERROR", "Unable to commit; operation rolled back");
    }
    for (JsonObject event : context.events) {
      try {
        publisher.accept(event.deepCopy());
      } catch (RuntimeException e) {
        System.err.println("Event delivery failed; reconcile after reconnect: " + e.getMessage());
      }
    }
    context.events.clear();
    return response.deepCopy();
  }

  /** Cập nhật trạng thái phiên theo lịch dưới cùng khóa với yêu cầu của máy khách. */
  public synchronized void tick() {
    execute("PING", "", "", new JsonObject());
  }

  // Chuyển yêu cầu cho lớp nghiệp vụ khi đang giữ khóa của dịch vụ.
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
        JsonObject a = context.auction(p);
        JsonObject rule = bidding.rule(user, a);
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
            context
                .collection("commands")
                .getAsJsonObject(text(user, "id") + ":" + required(p, "commandId", 100));
        yield c == null
            ? obj("status", "NOT_FOUND")
            : obj("status", text(c, "status"), "response", c.get("response").deepCopy());
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
        if (!text(a, "status").equals("CANCELED")) {
          throw error("CONFLICT", "Only canceled auctions may be archived");
        }
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
        "bidder_vip99", "Bidder12345", "Người đấu giá 2", "vip@auction.vn", "BIDDER", 300_000_000);
    JsonObject seller =
        auth.createUser(
            "seller_hieu", "Seller12345", "Người bán", "seller@auction.vn", "SELLER", 0);
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
