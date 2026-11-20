package com.auction.server.model;

import java.time.LocalDateTime;

/** Ghi một lượt đặt giá đã được chấp nhận vào lịch sử đấu giá. */
public record BidTransaction(
    String id,
    String auctionId,
    String bidderUsername,
    long amount,
    LocalDateTime timestamp,
    boolean automatic) {}
