package com.auction.server.assistant;

import java.time.LocalDateTime;
import java.util.List;

/** Read-only product and auction facts selected on the server for the assistant. */
public record AuctionProduct(
    String auctionId,
    String productId,
    String name,
    String category,
    String description,
    String condition,
    String status,
    long startingPrice,
    long currentPrice,
    long minBidStep,
    long minNextBid,
    long totalBids,
    boolean acceptsBids,
    LocalDateTime startTime,
    LocalDateTime endTime,
    List<BidPoint> recentBids) {
  /** Copies the bounded history so it remains independent of the transaction. */
  public AuctionProduct {
    recentBids = List.copyOf(recentBids);
  }

  /** A public price and time without the account identity or wallet data. */
  public record BidPoint(long amount, LocalDateTime timestamp) {}

  /** Indicates whether a bid was recorded for this auction. */
  public boolean hasBids() {
    return totalBids > 0;
  }
}
