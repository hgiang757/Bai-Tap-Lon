package com.auction.server.service;

import static com.auction.server.service.AuctionContext.error;
import static com.auction.server.service.AuctionContext.money;
import static com.auction.server.service.AuctionContext.number;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.dao.AuctionJsonMapper;
import com.auction.server.model.Auction;
import com.auction.server.model.BidTransaction;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Dùng chung quy tắc chấp nhận giá, giữ tiền và gia hạn cho đặt giá tay và tự động. */
final class BiddingLogic {
  private final AuctionContext context;
  private final WalletLogic wallets;

  BiddingLogic(AuctionContext context, WalletLogic wallets) {
    this.context = context;
    this.wallets = wallets;
  }

  private void validateBidder(JsonObject user, JsonObject a) {
    AuctionJsonMapper.auction(a).requireBidder(text(user, "id"), text(user, "role"), context.now());
  }

  // Kiểm tra phiên, quyền, giá và số dư trước khi chấp nhận rồi xử lý đặt giá tự động.
  JsonObject placeBid(JsonObject user, JsonObject p) {
    JsonObject a = context.auction(p);
    validateBidder(user, a);
    long amount = money(p, "amount");
    AuctionJsonMapper.auction(a).requireMinimumBid(amount);
    if (wallets.availableForAuction(user, a) < amount) {
      throw error("INSUFFICIENT_FUNDS", "Insufficient available funds");
    }
    acceptBid(a, user, amount, false);
    processAutoBids(a);
    return obj("auction", context.snapshot(a), "bids", a.getAsJsonArray("bids").deepCopy());
  }

  private void acceptBid(JsonObject a, JsonObject user, long amount, boolean automatic) {
    Auction previousAuction = AuctionJsonMapper.auction(a);
    String previous = previousAuction.highestBidderId();
    long previousAmount = previousAuction.currentPrice();
    Auction accepted = previousAuction.withBid(text(user, "id"), amount);
    AuctionJsonMapper.applyBid(a, accepted, text(user, "username"));
    if (!previous.isEmpty()) {
      wallets.log(
          context.collection("users").getAsJsonObject(previous),
          "BID_RELEASE",
          previousAmount,
          text(a, "id"));
    }
    wallets.log(user, "BID_HOLD", amount, text(a, "id"));
    BidTransaction bid =
        new BidTransaction(
            context.id(), text(a, "id"), text(user, "username"), amount, context.now(), automatic);
    a.getAsJsonArray("bids").add(AuctionJsonMapper.bid(bid));
    LocalDateTime end = accepted.endTime();
    LocalDateTime extended = new AntiSnipingPolicy().computeNewEndTime(end, context.now());
    if (!end.equals(extended)) {
      a.addProperty("endTime", extended.toString());
      context.event("AUCTION_EXTENDED", a, "");
    }
    context.event("BID_PLACED", a, "");
  }

  private String ruleKey(JsonObject user, JsonObject a) {
    return text(a, "id") + ":" + text(user, "id");
  }

  JsonObject rule(JsonObject u, JsonObject a) {
    return context.collection("rules").getAsJsonObject(ruleKey(u, a));
  }

  JsonObject setAuto(JsonObject user, JsonObject p) {
    JsonObject a = context.auction(p);
    validateBidder(user, a);
    long max = money(p, "maxBid");
    long increment = money(p, "increment");
    AuctionJsonMapper.auction(a).requireAutoBid(max, increment);
    if (wallets.availableForAuction(user, a) < max) {
      throw error("INSUFFICIENT_FUNDS", "Maximum exceeds available budget");
    }
    JsonObject r =
        obj(
            "id",
            context.id(),
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
            context.now(),
            "order",
            number(context.state, "version") + 1,
            "active",
            true,
            "limitReached",
            false,
            "autoCount",
            0);
    JsonObject existing = rule(user, a);
    if (existing != null) {
      // Cập nhật cấu hình đặt giá và bảo toàn các trường mở rộng đã lưu.
      r.entrySet().forEach(field -> existing.add(field.getKey(), field.getValue()));
      r = existing;
    }
    context.collection("rules").add(ruleKey(user, a), r);
    context.event("AUTO_BID_CHANGED", a, text(user, "id"));
    processAutoBids(a);
    return r.deepCopy();
  }

  // Mỗi vòng chọn một lượt đặt giá hợp lệ theo thứ tự đăng ký.
  private void processAutoBids(JsonObject a) {
    List<JsonObject> rules =
        context.values("rules").stream()
            .filter(r -> text(r, "auctionId").equals(text(a, "id")))
            .sorted(Comparator.comparingLong(r -> number(r, "order")))
            .toList();
    Map<JsonObject, Integer> priority = new IdentityHashMap<>();
    for (int i = 0; i < rules.size(); i++) {
      priority.put(rules.get(i), i);
    }
    PriorityQueue<JsonObject> registered =
        new PriorityQueue<>(Comparator.comparingInt(priority::get));
    registered.addAll(rules);
    for (int round = 0; round < 10_000; round++) {
      boolean placed = false;
      // Lấy cấu hình theo độ ưu tiên vì bộ lặp của hàng đợi không bảo đảm thứ tự đó.
      // Chỉ số giữ thứ tự cũ khi các cấu hình đã lưu có cùng độ ưu tiên.
      PriorityQueue<JsonObject> candidates = new PriorityQueue<>(registered);
      while (!candidates.isEmpty()) {
        JsonObject r = candidates.remove();
        Auction auction = AuctionJsonMapper.auction(a);
        if (!r.get("active").getAsBoolean() || auction.isLeading(text(r, "userId"))) {
          continue;
        }
        JsonObject user = context.collection("users").getAsJsonObject(text(r, "userId"));
        long minimum = auction.minimumNextBid();
        long max = number(r, "maxBid");
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
        long price = Math.min(max, auction.currentPrice() + number(r, "increment"));
        if (price == max
            && rules.stream()
                .anyMatch(
                    other ->
                        other != r
                            && other.get("active").getAsBoolean()
                            && number(other, "maxBid") == max
                            && wallets.availableForAuction(
                                    context
                                        .collection("users")
                                        .getAsJsonObject(text(other, "userId")),
                                    a)
                                >= max
                            && number(other, "order") < number(r, "order"))) {
          r.addProperty("active", false);
          r.addProperty("limitReached", true);
          continue;
        }
        if (wallets.availableForAuction(user, a) < price) {
          continue;
        }
        acceptBid(a, user, price, true);
        r.addProperty("autoCount", number(r, "autoCount") + 1);
        if (price == max) {
          r.addProperty("active", false);
          r.addProperty("limitReached", true);
        }
        placed = true;
        break;
      }
      if (!placed) {
        return;
      }
    }
    // Hoàn tác giao dịch nếu chưa xử lý hết lượt cạnh tranh tự động.
    throw error("AUTO_BID_LIMIT", "Too many auto-bid rounds; increase the increment");
  }
}
