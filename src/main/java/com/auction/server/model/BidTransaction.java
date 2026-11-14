package com.auction.server.model;

import java.time.LocalDateTime;

/** One accepted bid in the audit history; wallet transfers remain separate transactions. */
public record BidTransaction(
    String id,
    String auctionId,
    String bidderUsername,
    long amount,
    LocalDateTime timestamp,
    boolean automatic) {}
