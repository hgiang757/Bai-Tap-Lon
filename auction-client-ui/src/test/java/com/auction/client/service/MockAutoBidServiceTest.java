package com.auction.client.service;

import static org.junit.jupiter.api.Assertions.*;
import com.auction.client.model.*;
import com.auction.client.service.mock.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/** Kiểm tra cạnh tranh giá tự động và ví dùng chung. */
class MockAutoBidServiceTest {
    private record Setup(MockDataStore store, MockWalletService wallet, MockAuctionClientService alice,
                         MockAuctionClientService bob, MockAutoBidService autoAlice, MockAutoBidService autoBob, Auction auction) { }
    private Setup setup() {
        var store=MockDataStore.createForTesting(); var wallet=new MockWalletService(store);
        var alice=new MockAuctionClientService(store); var bob=new MockAuctionClientService(store);
        assertTrue(alice.register("alice", "x", "Alice", "alice@example.vn", UserRole.BIDDER));
        assertTrue(bob.register("bob", "x", "Bob", "bob@example.vn", UserRole.BIDDER));
        assertTrue(alice.createItemAndAuction(new Item("i-auto", "Sản phẩm", "Mô tả", "Khác", 1000, "seller"), 60, 100));
        Auction auction=alice.getAuctions(null).stream().filter(a -> a.getItem().getId().equals("i-auto")).findFirst().orElseThrow();
        return new Setup(store,wallet,alice,bob,new MockAutoBidService(store, () -> alice),new MockAutoBidService(store, () -> bob),auction);
    }
    @Test void earlierRegistrationWinsEqualMaximumAndNeverExceedsIt() {
        var t=setup(); String id=t.auction.getId();
        t.autoAlice.registerAutoBid(id, 1400, 100);
        t.autoBob.registerAutoBid(id, 1400, 100);
        assertEquals("alice", t.auction.getHighestBidderUsername());
        assertTrue(t.auction.getCurrentPrice() <= 1400);
        var history=t.alice.getBidHistory(id);
        assertTrue(history.stream().allMatch(Bid::isAuto));
        for (int i=1; i<history.size(); i++) assertEquals(100, history.get(i).getAmount()-history.get(i-1).getAmount());
        assertEquals((long)t.auction.getCurrentPrice(), t.wallet.getHeldAmount("alice"));
        assertEquals(0, t.wallet.getHeldAmount("bob"));
        assertFalse(t.autoBob.getMyAutoBid(id).isActive());
    }
    @Test void noSelfOutbiddingAndStopsAtCeiling() {
        var t=setup(); String id=t.auction.getId();
        var rule=t.autoAlice.registerAutoBid(id, 1400, 200);
        assertEquals(1, rule.getAutoCount()); assertEquals(1200, t.auction.getCurrentPrice());
        assertEquals(1, t.alice.getBidHistory(id).size());
        assertTrue(t.bob.placeBid(id, 1300).isSuccess());
        assertEquals(1400, t.auction.getCurrentPrice());
        assertFalse(t.autoAlice.getMyAutoBid(id).isActive());
        assertEquals(2, t.autoAlice.getMyAutoBid(id).getAutoCount());
        assertTrue(t.bob.placeBid(id, 1500).isSuccess());
        assertEquals("bob", t.auction.getHighestBidderUsername());
        assertEquals(0, t.wallet.getHeldAmount("alice")); assertEquals(1500, t.wallet.getHeldAmount("bob"));
    }
    @Test void notifiesListenersOnOtherServiceInstances() {
        var t=setup(); String id=t.auction.getId();
        var received=new ArrayList<Bid>();
        java.util.function.Consumer<Bid> listener=received::add;
        t.bob.registerBidListener(id, listener);
        t.autoAlice.registerAutoBid(id, 2000, 100);
        assertEquals(1, received.size()); assertTrue(received.get(0).isAuto());
        t.bob.unregisterBidListener(id, listener);
        assertTrue(t.bob.placeBid(id, 1200).isSuccess());
        assertEquals(1, received.size());
    }
    @Test void validatesLimitsAndCancellation() {
        var t=setup(); String id=t.auction.getId();
        assertEquals("Giá tối đa phải lớn hơn giá hiện tại ít nhất một bước giá.", assertThrows(AutoBidException.class, () -> t.autoAlice.registerAutoBid(id, 1000, 100)).getMessage());
        assertEquals("Bước giá phải từ 100 ₫ trở lên.", assertThrows(AutoBidException.class, () -> t.autoAlice.registerAutoBid(id, 2000, 10)).getMessage());
        assertEquals("Số dư khả dụng không đủ cho giá tối đa.", assertThrows(AutoBidException.class, () -> t.autoAlice.registerAutoBid(id, 60_000_000, 100)).getMessage());
        t.autoAlice.registerAutoBid(id, 2000, 100); t.autoAlice.cancelAutoBid(id);
        assertFalse(t.autoAlice.getMyAutoBid(id).isActive());
        t.auction.setEndTime(java.time.LocalDateTime.now().minusSeconds(1));
        assertEquals("Phiên đã kết thúc.", assertThrows(AutoBidException.class, () -> t.autoBob.registerAutoBid(id, 2000, 100)).getMessage());
    }
    @Test void concurrentManualBidsRemainMonotonicAndHoldOneWinner() throws Exception {
        var t=setup(); String id=t.auction.getId();
        t.autoAlice.registerAutoBid(id, 10_000, 100);
        var pool=Executors.newFixedThreadPool(8); var jobs=new ArrayList<Future<?>>();
        try {
            for (int i=0;i<80;i++) { long price=1100L+i*100; jobs.add(pool.submit(() -> t.bob.placeBid(id,price))); }
            for (var job:jobs) job.get();
        } finally { pool.shutdownNow(); }
        var history=t.alice.getBidHistory(id);
        for (int i=1;i<history.size();i++) assertTrue(history.get(i).getAmount()>history.get(i-1).getAmount());
        assertEquals(history.size(), t.auction.getTotalBids());
        assertEquals(history.get(history.size()-1).getBidderUsername(), t.auction.getHighestBidderUsername());
        assertEquals((long)t.auction.getCurrentPrice(), t.wallet.getHeldAmount("alice")+t.wallet.getHeldAmount("bob"));
        assertTrue(t.wallet.getHeldAmount("alice")==0 || t.wallet.getHeldAmount("bob")==0);
    }
}
