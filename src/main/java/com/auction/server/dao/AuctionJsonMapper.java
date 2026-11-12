package com.auction.server.dao;

import static com.auction.shared.JsonProtocol.number;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.model.Auction;
import com.auction.server.model.BidTransaction;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;

/** Maps domain values to schemaVersion=1 without rewriting unrelated snapshot fields. */
public final class AuctionJsonMapper {
    private AuctionJsonMapper() {}

    public static Auction auction(JsonObject json) {
        return new Auction(
                text(json, "id"),
                text(json, "sellerId"),
                number(json, "currentPrice"),
                number(json, "minBidStep"),
                text(json, "status"),
                LocalDateTime.parse(text(json, "startTime")),
                LocalDateTime.parse(text(json, "endTime")));
    }

    public static JsonObject bid(BidTransaction bid) {
        return obj(
                "id", bid.id(),
                "auctionId", bid.auctionId(),
                "bidderUsername", bid.bidderUsername(),
                "amount", bid.amount(),
                "timestamp", bid.timestamp(),
                "auto", bid.automatic());
    }
}
