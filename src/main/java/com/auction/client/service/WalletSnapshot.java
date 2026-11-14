package com.auction.client.service;

import com.auction.client.model.Transaction;
import java.util.List;

/** Contains total, held and available balances with immutable transaction history. */
public record WalletSnapshot(
    long balance, long held, long available, List<Transaction> transactions) {
  /** Copies transaction history so callers cannot change this snapshot. */
  public WalletSnapshot {
    transactions = List.copyOf(transactions);
  }
}
