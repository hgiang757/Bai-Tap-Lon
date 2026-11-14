package com.auction.server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reject damaged snapshots before a request or scheduler can modify any financial state. */
final class AuctionStateValidator {
  private AuctionStateValidator() {}

  static void validate(JsonObject state) throws IOException {
    try {
      require(integer(state, "schemaVersion") == 1, "schemaVersion");
      nonnegative(state, "version");
      JsonObject users = state.getAsJsonObject("users");
      JsonObject auctions = state.getAsJsonObject("auctions");
      JsonObject rules = state.getAsJsonObject("rules");
      JsonObject commands = state.getAsJsonObject("commands");
      require(
          users != null && auctions != null && rules != null && commands != null, "collections");
      Set<String> usernames = new HashSet<>();
      Set<String> emails = new HashSet<>();
      for (var entry : users.entrySet()) {
        JsonObject user = entry.getValue().getAsJsonObject();
        identity(entry.getKey(), user);
        require(usernames.add(string(user, "username").toLowerCase(Locale.ROOT)), "username");
        require(emails.add(string(user, "email").toLowerCase(Locale.ROOT)), "email");
        string(user, "fullName");
        oneOf(user, "role", "ADMIN", "BIDDER", "SELLER");
        String[] hash = string(user, "passwordHash").split(":", -1);
        require(
            hash.length == 2
                && Base64.getDecoder().decode(hash[0]).length == 16
                && Base64.getDecoder().decode(hash[1]).length == 32,
            "passwordHash");
        nonnegative(user, "balance");
        bool(user, "locked");
        bool(user, "deleted");
        time(user, "createdAt");
      }
      Map<String, Long> holds = new HashMap<>();
      Set<String> bidIds = new HashSet<>();
      for (var entry : auctions.entrySet()) {
        JsonObject auction = entry.getValue().getAsJsonObject();
        identity(entry.getKey(), auction);
        for (String field :
            List.of(
                "itemId",
                "name",
                "description",
                "sellerUsername",
                "highestBidderUsername",
                "imageBase64",
                "imageType")) {
          string(auction, field);
        }
        JsonObject seller = reference(users, auction, "sellerId");
        require(
            string(seller, "username").equals(string(auction, "sellerUsername")), "sellerUsername");
        oneOf(
            auction,
            "category",
            "ELECTRONICS",
            "ART",
            "VEHICLE",
            "JEWELRY",
            "REAL_ESTATE",
            "OTHER");
        final String status =
            oneOf(auction, "status", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED");
        require(time(auction, "endTime").isAfter(time(auction, "startTime")), "auction time");
        final long startingPrice = positive(auction, "startingPrice");
        positive(auction, "minBidStep");
        final long current = positive(auction, "currentPrice");
        bool(auction, "deleted");
        boolean settled = bool(auction, "settled");
        require(settled == Set.of("FINISHED", "PAID", "CANCELED").contains(status), "settled");
        if (auction.has("version")) {
          nonnegative(auction, "version");
        }
        JsonArray bids = auction.getAsJsonArray("bids");
        require(bids != null && integer(auction, "totalBids") == bids.size(), "totalBids");
        String lastBidder = "";
        long previous = startingPrice;
        for (JsonElement value : bids) {
          JsonObject bid = value.getAsJsonObject();
          require(bidIds.add(string(bid, "id")), "duplicate bid");
          require(entry.getKey().equals(string(bid, "auctionId")), "bid auction");
          long amount = positive(bid, "amount");
          require(amount > previous, "bid order");
          previous = amount;
          lastBidder = string(bid, "bidderUsername");
          require(usernames.contains(lastBidder.toLowerCase(Locale.ROOT)), "bidder");
          time(bid, "timestamp");
          bool(bid, "auto");
        }
        require(current == previous, "currentPrice");
        String leader = string(auction, "highestBidderId");
        require(leader.isEmpty() == bids.isEmpty(), "highestBidderId");
        if (!leader.isEmpty()) {
          JsonObject buyer = reference(users, auction, "highestBidderId");
          require(
              !leader.equals(string(auction, "sellerId"))
                  && lastBidder.equals(string(buyer, "username"))
                  && lastBidder.equals(string(auction, "highestBidderUsername")),
              "leader");
          if (!settled) {
            holds.merge(leader, current, Math::addExact);
          }
        }
        require(!status.equals("PAID") || !leader.isEmpty(), "paid winner");
        require(!status.equals("FINISHED") || leader.isEmpty(), "unpaid winner");
      }
      for (var hold : holds.entrySet()) {
        require(
            hold.getValue() <= integer(users.getAsJsonObject(hold.getKey()), "balance"),
            "wallet holds");
      }
      for (var entry : rules.entrySet()) {
        JsonObject rule = entry.getValue().getAsJsonObject();
        string(rule, "id");
        JsonObject user = reference(users, rule, "userId");
        reference(auctions, rule, "auctionId");
        require(
            entry.getKey().equals(string(rule, "auctionId") + ":" + string(rule, "userId")),
            "rule key");
        require(string(rule, "username").equals(string(user, "username")), "rule username");
        positive(rule, "maxBid");
        positive(rule, "increment");
        nonnegative(rule, "order");
        nonnegative(rule, "autoCount");
        bool(rule, "active");
        bool(rule, "limitReached");
        time(rule, "createdAt");
      }
      for (JsonElement value : commands.asMap().values()) {
        JsonObject command = value.getAsJsonObject();
        require(!string(command, "fingerprint").isEmpty(), "command fingerprint");
        String status = oneOf(command, "status", "SUCCEEDED", "REJECTED");
        JsonObject response = command.getAsJsonObject("response");
        require(
            response != null && bool(response, "success") == status.equals("SUCCEEDED"),
            "command response");
      }
      JsonArray transactions = state.getAsJsonArray("transactions");
      require(transactions != null, "transactions");
      Set<String> transactionIds = new HashSet<>();
      for (JsonElement value : transactions) {
        JsonObject transaction = value.getAsJsonObject();
        require(transactionIds.add(string(transaction, "id")), "duplicate transaction");
        JsonObject user = reference(users, transaction, "userId");
        require(
            string(transaction, "username").equals(string(user, "username")),
            "transaction username");
        oneOf(
            transaction,
            "type",
            "DEPOSIT",
            "WITHDRAW",
            "BID_HOLD",
            "BID_RELEASE",
            "PAYMENT",
            "SALE",
            "REFUND");
        require(integer(transaction, "amount") > 0, "transaction amount");
        nonnegative(transaction, "balanceAfter");
        time(transaction, "timestamp");
        string(transaction, "status");
        string(transaction, "note");
        String auctionId = string(transaction, "auctionId");
        require(auctionId.isEmpty() || auctions.has(auctionId), "transaction auction");
      }
    } catch (RuntimeException e) {
      throw new IOException(
          "Invalid saved state; refusing to overwrite data (" + e.getMessage() + ")", e);
    }
  }

  private static void require(boolean valid, String field) {
    if (!valid) {
      throw new IllegalArgumentException("invalid " + field);
    }
  }

  private static JsonPrimitive primitive(JsonObject object, String field) {
    JsonElement value = object.get(field);
    require(value != null && value.isJsonPrimitive(), field);
    return value.getAsJsonPrimitive();
  }

  private static String string(JsonObject object, String field) {
    JsonPrimitive value = primitive(object, field);
    require(value.isString(), field);
    return value.getAsString();
  }

  private static boolean bool(JsonObject object, String field) {
    JsonPrimitive value = primitive(object, field);
    require(value.isBoolean(), field);
    return value.getAsBoolean();
  }

  private static long integer(JsonObject object, String field) {
    JsonPrimitive value = primitive(object, field);
    require(value.isNumber(), field);
    return value.getAsBigDecimal().longValueExact();
  }

  private static long nonnegative(JsonObject object, String field) {
    long value = integer(object, field);
    require(value >= 0, field);
    return value;
  }

  private static long positive(JsonObject object, String field) {
    long value = integer(object, field);
    require(value > 0 && value <= AuctionService.MAX_MONEY, field);
    return value;
  }

  private static LocalDateTime time(JsonObject object, String field) {
    return LocalDateTime.parse(string(object, field));
  }

  private static String oneOf(JsonObject object, String field, String... choices) {
    String value = string(object, field);
    require(Arrays.asList(choices).contains(value), field);
    return value;
  }

  private static void identity(String key, JsonObject object) {
    require(!key.isEmpty() && key.equals(string(object, "id")), "entity id");
  }

  private static JsonObject reference(JsonObject collection, JsonObject object, String field) {
    JsonObject target = collection.getAsJsonObject(string(object, field));
    require(target != null, field);
    return target;
  }
}
