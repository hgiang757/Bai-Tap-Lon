package com.auction.client.service.mock;

import com.auction.client.model.Transaction;
import com.auction.client.model.TransactionType;
import com.auction.client.service.WalletException;
import com.auction.client.service.WalletService;
import java.util.List;

/** Ví mô phỏng, không phải tiền thật. */
public final class MockWalletService implements WalletService {
    private final MockDataStore store;
    /** Khởi tạo với kho dùng chung. */
    public MockWalletService() { this(MockDataStore.getInstance()); }
    /** Khởi tạo với kho được chỉ định. */
    public MockWalletService(MockDataStore store) { this.store = store; }
    @Override public long getBalance(String user) { synchronized (store) { store.refreshStatuses(); return store.balance(user); } }
    @Override public long getHeldAmount(String user) { synchronized (store) { store.refreshStatuses(); return store.held(user); } }
    @Override public long getAvailable(String user) { synchronized (store) { store.refreshStatuses(); return store.balance(user) - store.held(user); } }
    @Override public void deposit(String user, long amount) { synchronized (store) {
        if (amount <= 0) throw new WalletException("Số tiền phải là số nguyên dương.");
        if (amount < 10_000) throw new WalletException("Số tiền nạp tối thiểu 10.000 ₫.");
        try { store.changeBalance(user, Math.addExact(store.balance(user), amount), TransactionType.DEPOSIT, amount, null); }
        catch (ArithmeticException ex) { throw new WalletException("Số tiền vượt giới hạn cho phép."); }
    } }
    @Override public void withdraw(String user, long amount) { synchronized (store) {
        if (amount <= 0) throw new WalletException("Số tiền phải là số nguyên dương.");
        store.refreshStatuses();
        if (store.balance(user) - store.held(user) < amount) throw new WalletException("Số dư khả dụng không đủ để rút.");
        store.changeBalance(user, store.balance(user) - amount, TransactionType.WITHDRAW, amount, null);
    } }
    @Override public List<Transaction> getTransactions(String user) { synchronized (store) { store.refreshStatuses(); return store.transactions(user); } }
}
