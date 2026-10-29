package com.auction.client.service;

import com.auction.client.model.Transaction;
import java.util.List;

/** Dịch vụ ví mô phỏng, không phải tiền thật. */
public interface WalletService {
    /** Số dư tổng. */ long getBalance(String username);
    /** Số tiền đang giữ. */ long getHeldAmount(String username);
    /** Số dư khả dụng. */ long getAvailable(String username);
    /** Nạp tiền mô phỏng. */ void deposit(String username, long amount);
    /** Rút tiền mô phỏng. */ void withdraw(String username, long amount);
    /** Danh sách giao dịch mới nhất trước. */ List<Transaction> getTransactions(String username);
}
