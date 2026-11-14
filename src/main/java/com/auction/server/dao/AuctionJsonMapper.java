package com.auction.server.dao;

import static com.auction.shared.JsonProtocol.number;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;

/** Maps domain values to schemaVersion=1 without rewriting unrelated snapshot fields. */
public final class AuctionJsonMapper {
  private AuctionJsonMapper() {}

  /** Builds an immutable domain auction from the stored JSON fields. */
  public static Auction auction(JsonObject json) {
    return new Auction(
        text(json, "id"),
        text(json, "sellerId"),
        number(json, "currentPrice"),
        number(json, "minBidStep"),
        AuctionStatus.valueOf(text(json, "status")),
        LocalDateTime.parse(text(json, "startTime")),
        LocalDateTime.parse(text(json, "endTime")),
        text(json, "highestBidderId"),
        number(json, "totalBids"),
        json.get("settled").getAsBoolean());
  }

  /** Merge only bidding fields into the existing JSON tree, retaining all extension fields. */
  public static void applyBid(JsonObject target, Auction accepted, String username) {
    target.addProperty("highestBidderId", accepted.highestBidderId());
    target.addProperty("highestBidderUsername", username);
    target.addProperty("currentPrice", accepted.currentPrice());
    target.addProperty("totalBids", accepted.totalBids());
  }

  /** Writes the accepted domain bid using the existing JSON field names. */
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
