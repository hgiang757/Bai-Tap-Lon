package com.auction.server.assistant;

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
    boolean hasBids) {}
