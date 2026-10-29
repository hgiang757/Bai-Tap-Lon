package com.auction.client.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Giao dịch ví mô phỏng. */
public record Transaction(String id, String username, TransactionType type, long amount,
                          long balanceAfter, LocalDateTime timestamp, String status,
                          String note, String auctionId) implements Serializable { }
