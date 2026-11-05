package com.auction.client.service.server;

import com.auction.client.model.*;
import com.google.gson.*;

import java.time.LocalDateTime;

final class ServerMapper {
    private ServerMapper() {}

    static String s(JsonObject o, String k) {
        return !o.has(k) || o.get(k).isJsonNull() ? "" : o.get(k).getAsString();
    }

    static long n(JsonObject o, String k) {
        return o.get(k).getAsLong();
    }

    static LocalDateTime time(JsonObject o, String k) {
        return LocalDateTime.parse(s(o, k));
    }

    static User user(JsonObject o) {
        return new User(
                s(o, "id"),
                s(o, "username"),
                s(o, "fullName"),
                s(o, "email"),
                UserRole.valueOf(s(o, "role")),
                n(o, "balance"));
    }

    static Auction auction(JsonObject o) {
        Item item =
                new Item(
                        s(o, "itemId"),
                        s(o, "name"),
                        s(o, "description"),
                        s(o, "category"),
                        n(o, "startingPrice"),
                        s(o, "sellerUsername"));
        Auction a =
                new Auction(
                        s(o, "id"),
                        item,
                        s(o, "sellerUsername"),
                        n(o, "currentPrice"),
                        s(o, "highestBidderUsername").isBlank()
                                ? null
                                : s(o, "highestBidderUsername"),
                        AuctionStatus.valueOf(s(o, "status")),
                        time(o, "startTime"),
                        time(o, "endTime"),
                        n(o, "minBidStep"));
        a.setTotalBids((int) n(o, "totalBids"));
        a.setCancellationReason(s(o, "cancellationReason"));
        return a;
    }

    static Bid bid(JsonObject o) {
        return new Bid(
                s(o, "id"),
                s(o, "auctionId"),
                s(o, "bidderUsername"),
                n(o, "amount"),
                time(o, "timestamp"),
                o.get("auto").getAsBoolean());
    }

    static AutoBid auto(JsonObject o) {
        AutoBid r =
                new AutoBid(
                        s(o, "id"),
                        s(o, "auctionId"),
                        s(o, "username"),
                        n(o, "maxBid"),
                        n(o, "increment"),
                        time(o, "createdAt"));
        r.restoreState(
                o.get("active").getAsBoolean(),
                o.get("limitReached").getAsBoolean(),
                (int) n(o, "autoCount"));
        return r;
    }

    static Transaction transaction(JsonObject o) {
        return new Transaction(
                s(o, "id"),
                s(o, "username"),
                TransactionType.valueOf(s(o, "type")),
                n(o, "amount"),
                n(o, "balanceAfter"),
                time(o, "timestamp"),
                s(o, "status"),
                s(o, "note"),
                s(o, "auctionId"));
    }
}
