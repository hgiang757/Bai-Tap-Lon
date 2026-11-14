package com.auction.client.service.server;

import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.AutoBid;
import com.auction.client.model.Bid;
import com.auction.client.model.Item;
import com.auction.client.model.Transaction;
import com.auction.client.model.TransactionType;
import com.auction.client.model.User;
import com.auction.client.model.UserRole;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;

final class ServerMapper {
  private ServerMapper() {}

  static String text(JsonObject o, String k) {
    return !o.has(k) || o.get(k).isJsonNull() ? "" : o.get(k).getAsString();
  }

  static long longValue(JsonObject o, String k) {
    return o.get(k).getAsLong();
  }

  static LocalDateTime time(JsonObject o, String k) {
    return LocalDateTime.parse(text(o, k));
  }

  static User user(JsonObject o) {
    return new User(
        text(o, "id"),
        text(o, "username"),
        text(o, "fullName"),
        text(o, "email"),
        UserRole.valueOf(text(o, "role")),
        longValue(o, "balance"));
  }

  static Auction auction(JsonObject o) {
    Item item =
        new Item(
            text(o, "itemId"),
            text(o, "name"),
            text(o, "description"),
            text(o, "category"),
            longValue(o, "startingPrice"),
            text(o, "sellerUsername"));
    Auction a =
        new Auction(
            text(o, "id"),
            item,
            text(o, "sellerUsername"),
            longValue(o, "currentPrice"),
            text(o, "highestBidderUsername").isBlank() ? null : text(o, "highestBidderUsername"),
            AuctionStatus.valueOf(text(o, "status")),
            time(o, "startTime"),
            time(o, "endTime"),
            longValue(o, "minBidStep"));
    a.setTotalBids((int) longValue(o, "totalBids"));
    a.setCancellationReason(text(o, "cancellationReason"));
    return a;
  }

  static Bid bid(JsonObject o) {
    return new Bid(
        text(o, "id"),
        text(o, "auctionId"),
        text(o, "bidderUsername"),
        longValue(o, "amount"),
        time(o, "timestamp"),
        o.get("auto").getAsBoolean());
  }

  static AutoBid auto(JsonObject o) {
    AutoBid r =
        new AutoBid(
            text(o, "id"),
            text(o, "auctionId"),
            text(o, "username"),
            longValue(o, "maxBid"),
            longValue(o, "increment"),
            time(o, "createdAt"));
    r.restoreState(
        o.get("active").getAsBoolean(),
        o.get("limitReached").getAsBoolean(),
        (int) longValue(o, "autoCount"));
    return r;
  }

  static Transaction transaction(JsonObject o) {
    return new Transaction(
        text(o, "id"),
        text(o, "username"),
        TransactionType.valueOf(text(o, "type")),
        longValue(o, "amount"),
        longValue(o, "balanceAfter"),
        time(o, "timestamp"),
        text(o, "status"),
        text(o, "note"),
        text(o, "auctionId"));
  }
}
