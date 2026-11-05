package com.auction.client.service;

import com.auction.client.model.Transaction;
import java.util.List;

public record WalletSnapshot(long balance, long held, long available, List<Transaction> transactions) {
    public WalletSnapshot { transactions = List.copyOf(transactions); }
}
