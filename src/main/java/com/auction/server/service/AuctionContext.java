package com.auction.server.service;

import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.dao.AuctionJsonMapper;
import com.auction.server.exception.ApiException;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared snapshot and buffered events. Only AuctionService owns this context and its helpers. Every
 * request/tick uses the service monitor; helpers never lock, save or publish themselves. Rollback
 * replaces state here, so helpers cannot retain a discarded snapshot as their root.
 */
final class AuctionContext {
  static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  static final long MAX_MONEY = 9_000_000_000_000L;
  JsonObject state;
  List<JsonObject> events = new ArrayList<>();
  final Clock clock;

  AuctionContext(Clock clock) {
    this.clock = clock;
  }

  static long number(JsonObject object, String field) {
    try {
      return com.auction.shared.JsonProtocol.number(object, field);
    } catch (IllegalArgumentException e) {
      throw error("VALIDATION_ERROR", "Expected integer: " + field);
    }
  }

  static ApiException error(String code, String message) {
    return new ApiException(code, message);
  }

  static String required(JsonObject p, String key, int max) {
    String s = text(p, key).strip();
    if (s.isEmpty() || s.length() > max) {
      throw error("VALIDATION_ERROR", "Invalid " + key);
    }
    return s;
  }

  static long money(JsonObject p, String key) {
    long n = number(p, key);
    if (n <= 0 || n > MAX_MONEY) {
      throw error("VALIDATION_ERROR", "Invalid money: " + key);
    }
    return n;
  }

  static void requireRole(JsonObject user, String role) {
    if (!text(user, "role").equals(role)) {
      throw error("FORBIDDEN", "Requires " + role);
    }
  }

  JsonObject collection(String name) {
    return state.getAsJsonObject(name);
  }

  List<JsonObject> values(String name) {
    return collection(name).entrySet().stream().map(e -> e.getValue().getAsJsonObject()).toList();
  }

  LocalDateTime now() {
    return LocalDateTime.ofInstant(clock.instant(), ZONE);
  }

  String id() {
    return UUID.randomUUID().toString();
  }

  JsonObject auction(JsonObject p) {
    JsonObject a = collection("auctions").getAsJsonObject(required(p, "auctionId", 100));
    if (a == null || a.get("deleted").getAsBoolean()) {
      throw error("NOT_FOUND", "Auction not found");
    }
    return a;
  }

  JsonObject snapshot(JsonObject a) {
    JsonObject copy = a.deepCopy();
    copy.remove("bids");
    copy.remove("imageBase64");
    copy.addProperty("hasImage", !text(a, "imageBase64").isEmpty());
    copy.addProperty("minNextBid", AuctionJsonMapper.auction(a).minimumNextBid());
    return copy;
  }

  void event(String type, JsonObject auction, String userId) {
    long version = number(state, "version") + 1;
    state.addProperty("version", version);
    if (auction != null) {
      auction.addProperty("version", version);
    }
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
}
