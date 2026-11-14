package com.auction.server.service;

import static com.auction.server.service.AuctionContext.MAX_MONEY;
import static com.auction.server.service.AuctionContext.error;
import static com.auction.server.service.AuctionContext.money;
import static com.auction.server.service.AuctionContext.number;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.dao.AuctionJsonMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Wallet holds, available budget and audit entries, inside the service transaction only. */
final class WalletLogic {
  private final AuctionContext context;

  WalletLogic(AuctionContext context) {
    this.context = context;
  }

  // Tiền khả dụng = tổng số dư - tiền giữ ở các phiên đang dẫn đầu.
  JsonObject wallet(JsonObject user) {
    long total = number(user, "balance");
    long held = heldBalance(text(user, "id"));
    return obj(
        "balance",
        total,
        "held",
        held,
        "available",
        total - held,
        "version",
        number(context.state, "version"));
  }

  long heldBalance(String userId) {
    return context.values("auctions").stream()
        .map(AuctionJsonMapper::auction)
        .mapToLong(a -> a.heldBy(userId))
        .sum();
  }

  long availableForAuction(JsonObject u, JsonObject a) {
    var auction = AuctionJsonMapper.auction(a);
    return number(u, "balance")
        - heldBalance(text(u, "id"))
        + (auction.isLeading(text(u, "id")) ? auction.currentPrice() : 0);
  }

  void log(JsonObject user, String type, long amount, String auctionId) {
    context
        .state
        .getAsJsonArray("transactions")
        .add(
            obj(
                "id",
                context.id(),
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
                context.now(),
                "status",
                "Thành công",
                "note",
                "",
                "auctionId",
                auctionId));
    context.event("WALLET_CHANGED", null, text(user, "id"));
  }

  JsonArray history(JsonObject u) {
    JsonArray out = new JsonArray();
    JsonArray all = context.state.getAsJsonArray("transactions");
    for (int i = all.size() - 1; i >= 0; i--) {
      if (text(all.get(i).getAsJsonObject(), "userId").equals(text(u, "id"))) {
        out.add(all.get(i).deepCopy());
      }
    }
    return out;
  }

  JsonObject transfer(JsonObject user, JsonObject p, boolean deposit) {
    if (text(user, "role").equals("ADMIN")) {
      throw error("FORBIDDEN", "Admin cannot use participant wallet");
    }
    long amount = money(p, "amount");
    long balance = number(user, "balance");
    if (deposit && amount < 10_000) {
      throw error("VALIDATION_ERROR", "Minimum deposit is 10000 VND");
    }
    if (!deposit && amount > balance - heldBalance(text(user, "id"))) {
      throw error("INSUFFICIENT_FUNDS", "Insufficient available funds");
    }
    if (deposit && balance > MAX_MONEY - amount) {
      throw error("VALIDATION_ERROR", "Wallet limit exceeded");
    }
    user.addProperty("balance", deposit ? balance + amount : balance - amount);
    log(user, deposit ? "DEPOSIT" : "WITHDRAW", amount, "");
    return wallet(user);
  }
}
