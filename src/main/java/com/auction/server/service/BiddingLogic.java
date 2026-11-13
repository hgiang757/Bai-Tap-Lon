package com.auction.server.service;

import static com.auction.server.service.AuctionContext.*;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.dao.AuctionJsonMapper;
import com.auction.server.model.Auction;
import com.auction.server.model.BidTransaction;
import com.google.gson.*;

import java.time.LocalDateTime;
import java.util.*;

/** Manual bids and automatic competition share acceptance, wallet holds and anti-sniping. */
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

    // Đặt giá: kiểm tra phiên/quyền/giá/tiền, chấp nhận rồi chạy auto-bid.
    JsonObject placeBid(JsonObject user, JsonObject p) {
        JsonObject a = context.auction(p);
        validateBidder(user, a);
        long amount = money(p, "amount");
        AuctionJsonMapper.auction(a).requireMinimumBid(amount);
        if (wallets.availableForAuction(user, a) < amount)
            throw error("INSUFFICIENT_FUNDS", "Insufficient available funds");
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
        if (!previous.isEmpty())
            wallets.log(
                    context.collection("users").getAsJsonObject(previous),
                    "BID_RELEASE",
                    previousAmount,
                    text(a, "id"));
        wallets.log(user, "BID_HOLD", amount, text(a, "id"));
        BidTransaction bid =
                new BidTransaction(context.id(), text(a, "id"), text(user, "username"), amount, context.now(), automatic);
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
        long max = money(p, "maxBid"), increment = money(p, "increment");
        AuctionJsonMapper.auction(a).requireAutoBid(max, increment);
        if (wallets.availableForAuction(user, a) < max)
            throw error("INSUFFICIENT_FUNDS", "Maximum exceeds available budget");
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
            // Re-register known fields while preserving extension fields from the stored rule.
            r.entrySet().forEach(field -> existing.add(field.getKey(), field.getValue()));
            r = existing;
        }
        context.collection("rules").add(ruleKey(user, a), r);
        context.event("AUTO_BID_CHANGED", a, text(user, "id"));
        processAutoBids(a);
        return r.deepCopy();
    }

    // Theo thứ tự đăng ký; mỗi lượt chọn một bid hợp lệ, lặp tới khi dừng.
    private void processAutoBids(JsonObject a) {
        List<JsonObject> rules =
                context.values("rules").stream()
                        .filter(r -> text(r, "auctionId").equals(text(a, "id")))
                        .sorted(Comparator.comparingLong(r -> number(r, "order")))
                        .toList();
        Map<JsonObject, Integer> priority = new IdentityHashMap<>();
        for (int i = 0; i < rules.size(); i++) priority.put(rules.get(i), i);
        PriorityQueue<JsonObject> registered =
                new PriorityQueue<>(Comparator.comparingInt(priority::get));
        registered.addAll(rules);
        for (int round = 0; round < 10_000; round++) {
            boolean placed = false;
            // Poll the registration priority; a heap's iterator is not priority ordered.
            // List index preserves the previous stable order even for legacy equal-order rules.
            PriorityQueue<JsonObject> candidates = new PriorityQueue<>(registered);
            while (!candidates.isEmpty()) {
                JsonObject r = candidates.remove();
                Auction auction = AuctionJsonMapper.auction(a);
                if (!r.get("active").getAsBoolean()
                        || auction.isLeading(text(r, "userId"))) continue;
                JsonObject user = context.collection("users").getAsJsonObject(text(r, "userId"));
                long minimum = auction.minimumNextBid(),
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
                long price = Math.min(max, auction.currentPrice() + number(r, "increment"));
                if (price == max
                        && rules.stream()
                                .anyMatch(
                                        other ->
                                                other != r
                                                        && other.get("active").getAsBoolean()
                                                        && number(other, "maxBid") == max
                                                        && wallets.availableForAuction(
                                                                        context.collection("users").getAsJsonObject(text(other, "userId")), a)
                                                                >= max
                                                        && number(other, "order")
                                                                < number(r, "order"))) {
                    r.addProperty("active", false);
                    r.addProperty("limitReached", true);
                    continue;
                }
                if (wallets.availableForAuction(user, a) < price) continue;
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
}
