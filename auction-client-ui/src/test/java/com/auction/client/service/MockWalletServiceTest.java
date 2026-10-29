package com.auction.client.service;

import static org.junit.jupiter.api.Assertions.*;
import com.auction.client.model.*;
import com.auction.client.service.mock.*;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/** Kiểm tra ví và tích hợp giữ tiền trong đấu giá. */
class MockWalletServiceTest {
    @Test void depositWithdrawAndValidation() {
        var wallet = new MockWalletService(MockDataStore.createForTesting());
        assertThrows(WalletException.class, () -> wallet.deposit("a", 0));
        assertEquals("Số tiền nạp tối thiểu 10.000 ₫.", assertThrows(WalletException.class, () -> wallet.deposit("a", 9999)).getMessage());
        wallet.deposit("a", 100_000); wallet.withdraw("a", 20_000);
        assertEquals(80_000, wallet.getBalance("a"));
        assertEquals(80_000, wallet.getTransactions("a").get(0).balanceAfter());
        assertEquals("Số dư khả dụng không đủ để rút.", assertThrows(WalletException.class, () -> wallet.withdraw("a", 80_001)).getMessage());
    }
    @Test void holdsReleaseAndPaymentAreAtomic() {
        var store = MockDataStore.createForTesting(); var wallet = new MockWalletService(store);
        var first = new MockAuctionClientService(store); var second = new MockAuctionClientService(store);
        assertTrue(first.register("alice", "123", "Alice", "alice@test.vn", UserRole.BIDDER));
        assertTrue(second.register("bob", "123", "Bob", "bob@test.vn", UserRole.BIDDER));
        var item = new Item("test-wallet", "Món", "Mô tả", "Khác", 1000, "seller");
        assertTrue(first.createItemAndAuction(item, 60, 100));
        var auction = first.getAuctions(null).stream().filter(a -> a.getItem().getId().equals("test-wallet")).findFirst().orElseThrow();
        assertTrue(first.placeBid(auction.getId(), 1100).isSuccess());
        assertEquals(1100, wallet.getHeldAmount("alice"));
        assertEquals(50_000_000-1100, wallet.getAvailable("alice"));
        assertTrue(second.placeBid(auction.getId(), 1200).isSuccess());
        assertEquals(0, wallet.getHeldAmount("alice")); assertEquals(1200, wallet.getHeldAmount("bob"));
        assertEquals(TransactionType.BID_RELEASE, wallet.getTransactions("alice").get(0).type());
        assertFalse(first.placeBid(auction.getId(), 60_000_000).isSuccess());
        auction.setEndTime(LocalDateTime.now().minusSeconds(1));
        assertEquals(AuctionStatus.PAID, second.getAuctionById(auction.getId()).getStatus());
        assertEquals(0, wallet.getHeldAmount("bob")); assertEquals(50_000_000-1200, wallet.getBalance("bob"));
        assertEquals(TransactionType.PAYMENT, wallet.getTransactions("bob").get(0).type());
        assertEquals(wallet.getBalance("bob"), (long) second.getCurrentUser().getBalance());
    }
    @Test void cancellationReleasesFunds() {
        var store = MockDataStore.createForTesting();
        var wallet = new MockWalletService(store);
        var bidder = new MockAuctionClientService(store);
        var management = new MockManagementService(store);
        assertTrue(bidder.register("cancel-user", "123", "Tên", "cancel@test.vn", UserRole.BIDDER));
        var item = new Item("cancel-item", "Món", "Mô tả", "Khác", 1000, "seller");
        assertTrue(bidder.createItemAndAuction(item, 60, 100));
        var auction = bidder.getAuctions(null).stream().filter(a -> a.getItem().getId().equals("cancel-item")).findFirst().orElseThrow();
        assertTrue(bidder.placeBid(auction.getId(), 1100).isSuccess());
        management.cancelAuction(auction.getId(), "Hủy phiên");
        assertEquals(0, wallet.getHeldAmount("cancel-user"));
        assertEquals(TransactionType.BID_RELEASE, wallet.getTransactions("cancel-user").get(0).type());
    }
    @Test void concurrentDepositsDoNotLoseUpdates() throws Exception {
        var wallet = new MockWalletService(MockDataStore.createForTesting());
        var pool = Executors.newFixedThreadPool(8);
        try {
            var jobs = new java.util.ArrayList<Future<?>>();
            for (int i=0; i<100; i++) jobs.add(pool.submit(() -> wallet.deposit("parallel", 10_000)));
            for (var job : jobs) job.get();
            assertEquals(1_000_000, wallet.getBalance("parallel"));
            assertEquals(100, wallet.getTransactions("parallel").size());
        } finally { pool.shutdownNow(); }
    }
}
