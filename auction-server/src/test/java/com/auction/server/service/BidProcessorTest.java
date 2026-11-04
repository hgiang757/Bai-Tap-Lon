package com.auction.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.server.exception.AuctionClosedException;
import com.auction.server.exception.InvalidBidException;
import com.auction.server.model.Auction;
import com.auction.server.model.Bid;
import com.auction.server.model.Bidder;
import com.auction.server.model.Item;
import com.auction.server.model.Seller;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class BidProcessorTest {

    /** Item tối thiểu để dựng phiên đấu giá trong test. */
    private static class TestItem extends Item {
        private static final long serialVersionUID = 1L;

        TestItem() {
            super("item-1", "Món đồ thử", "Mô tả", (Seller) null);
        }

        @Override
        public double calculateInsuranceFee(double currentPrice) {
            return 0;
        }
    }

    // Đồng hồ cố định: 2026-11-10 10:00:00
    private final Clock clock =
            Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC);
    private final BidProcessor processor =
            new BidProcessor(new AntiSnipingPolicy(Duration.ofSeconds(30), Duration.ofSeconds(60)), clock);

    private final Bidder alice = new Bidder("u-alice", "alice", "pw");
    private final Bidder bob = new Bidder("u-bob", "bob", "pw");

    /** Phiên: giá khởi điểm 100, bước giá 10, kết thúc sau {@code secondsLeft} giây. */
    private Auction auction(long secondsLeft) {
        LocalDateTime end = LocalDateTime.of(2026, 11, 10, 10, 0, 0).plusSeconds(secondsLeft);
        return new Auction("a-1", new TestItem(), 100, 10, end);
    }

    // ------------------------------------------------------------ đặt giá thường

    @Test
    void firstBidAtStartingPrice_isAccepted() throws Exception {
        Auction a = auction(600);
        List<Bid> placed = processor.placeBid(a, alice, 100);

        assertEquals(1, placed.size());
        assertEquals(100, a.getCurrentPrice());
        assertSame(alice, a.getCurrentHighestBidder());
        assertEquals(BidProcessor.STATUS_RUNNING, a.getStatus());
    }

    @Test
    void firstBidBelowStartingPrice_throwsInvalidBid() {
        Auction a = auction(600);
        assertThrows(InvalidBidException.class, () -> processor.placeBid(a, alice, 99));
        assertTrue(a.getBidHistory().isEmpty());
    }

    @Test
    void bidBelowCurrentPlusStep_throwsInvalidBid() throws Exception {
        Auction a = auction(600);
        processor.placeBid(a, alice, 100);

        assertThrows(InvalidBidException.class, () -> processor.placeBid(a, bob, 105));
        assertThrows(InvalidBidException.class, () -> processor.placeBid(a, bob, 100));
        assertEquals(100, a.getCurrentPrice());
    }

    @Test
    void bidExactlyCurrentPlusStep_isAccepted() throws Exception {
        Auction a = auction(600);
        processor.placeBid(a, alice, 100);
        processor.placeBid(a, bob, 110);

        assertEquals(110, a.getCurrentPrice());
        assertSame(bob, a.getCurrentHighestBidder());
    }

    @Test
    void leaderBiddingAgain_throwsInvalidBid() throws Exception {
        Auction a = auction(600);
        processor.placeBid(a, alice, 100);
        assertThrows(InvalidBidException.class, () -> processor.placeBid(a, alice, 200));
    }

    @Test
    void nonPositiveOrNaNAmount_throwsInvalidBid() {
        Auction a = auction(600);
        assertThrows(InvalidBidException.class, () -> processor.placeBid(a, alice, 0));
        assertThrows(InvalidBidException.class, () -> processor.placeBid(a, alice, -5));
        assertThrows(InvalidBidException.class, () -> processor.placeBid(a, alice, Double.NaN));
    }

    @Test
    void nullArguments_throwInvalidBid() {
        Auction a = auction(600);
        assertThrows(InvalidBidException.class, () -> processor.placeBid(null, alice, 100));
        assertThrows(InvalidBidException.class, () -> processor.placeBid(a, null, 100));
    }

    // ------------------------------------------------------------ phiên đã đóng

    @Test
    void bidOnFinishedAuction_throwsAuctionClosed() {
        Auction a = auction(600);
        a.setStatus(BidProcessor.STATUS_FINISHED);
        assertThrows(AuctionClosedException.class, () -> processor.placeBid(a, alice, 100));
    }

    @Test
    void bidAfterEndTime_throwsAuctionClosedAndFinishesAuction() {
        Auction a = auction(-5);
        assertThrows(AuctionClosedException.class, () -> processor.placeBid(a, alice, 100));
        assertEquals(BidProcessor.STATUS_FINISHED, a.getStatus());
    }

    @Test
    void closeIfExpired_closesOnlyWhenTimeIsUp() {
        Auction running = auction(600);
        Auction expired = auction(0);

        assertFalse(processor.closeIfExpired(running));
        assertTrue(processor.closeIfExpired(expired));
        assertEquals(BidProcessor.STATUS_FINISHED, expired.getStatus());
    }

    @Test
    void finish_returnsHighestBidderAsWinner() throws Exception {
        Auction a = auction(600);
        processor.placeBid(a, alice, 100);
        processor.placeBid(a, bob, 120);

        Optional<Bidder> winner = processor.finish(a);

        assertTrue(winner.isPresent());
        assertSame(bob, winner.get());
        assertEquals(BidProcessor.STATUS_FINISHED, a.getStatus());
        assertThrows(AuctionClosedException.class, () -> processor.placeBid(a, alice, 200));
    }

    @Test
    void finishWithoutBids_hasNoWinner() {
        Auction a = auction(600);
        assertTrue(processor.finish(a).isEmpty());
        assertEquals(BidProcessor.STATUS_FINISHED, a.getStatus());
    }

    // ------------------------------------------------------------ anti-sniping

    @Test
    void bidInLastSeconds_extendsEndTime() throws Exception {
        Auction a = auction(10);
        LocalDateTime oldEnd = a.getEndTime();

        processor.placeBid(a, alice, 100);

        assertEquals(oldEnd.plusSeconds(60), a.getEndTime());
    }

    @Test
    void bidFarFromEnd_keepsEndTime() throws Exception {
        Auction a = auction(600);
        LocalDateTime oldEnd = a.getEndTime();

        processor.placeBid(a, alice, 100);

        assertEquals(oldEnd, a.getEndTime());
    }

    // ------------------------------------------------------------ auto-bid

    @Test
    void autoBid_outbidsManualBidAutomatically() throws Exception {
        Auction a = auction(600);
        processor.registerAutoBid(a, alice, 200, 10);   // chưa có bid nào -> đặt ngay 110

        List<Bid> placed = processor.placeBid(a, bob, 120);

        // bob 120, alice tự trả 130
        assertEquals(2, placed.size());
        assertSame(alice, a.getCurrentHighestBidder());
        assertEquals(130, a.getCurrentPrice());
    }

    @Test
    void autoBid_stopsAtMaxBid() throws Exception {
        Auction a = auction(600);
        processor.registerAutoBid(a, alice, 150, 10);
        processor.placeBid(a, bob, 150);   // bob 150, alice không đủ (150 + 10 > 150)

        assertSame(bob, a.getCurrentHighestBidder());
        assertEquals(150, a.getCurrentPrice());
    }

    @Test
    void registerAutoBid_placesBidImmediatelyWhenNotLeading() throws Exception {
        Auction a = auction(600);
        processor.placeBid(a, bob, 100);

        List<Bid> placed = processor.registerAutoBid(a, alice, 300, 20);

        assertEquals(1, placed.size());
        assertSame(alice, a.getCurrentHighestBidder());
        assertEquals(120, a.getCurrentPrice());
    }

    @Test
    void registerAutoBid_incrementBelowStep_throwsInvalidBid() {
        Auction a = auction(600);
        assertThrows(InvalidBidException.class, () -> processor.registerAutoBid(a, alice, 300, 5));
    }

    @Test
    void registerAutoBid_maxBelowMinimum_throwsInvalidBid() {
        Auction a = auction(600);
        assertThrows(InvalidBidException.class, () -> processor.registerAutoBid(a, alice, 50, 10));
    }

    @Test
    void cancelAutoBid_stopsFurtherAutoBids() throws Exception {
        Auction a = auction(600);
        processor.registerAutoBid(a, alice, 500, 10);
        processor.cancelAutoBid(a, alice);

        processor.placeBid(a, bob, 200);

        assertSame(bob, a.getCurrentHighestBidder());
        assertEquals(200, a.getCurrentPrice());
    }

    // ------------------------------------------------------------ đồng thời

    @Test
    void concurrentBids_neverLoseUpdates() throws Exception {
        final int bidders = 60;
        Auction a = auction(3600);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        List<Exception> unexpected = new ArrayList<>();

        for (int i = 0; i < bidders; i++) {
            final Bidder b = new Bidder("u-" + i, "user" + i, "pw");
            final double amount = 100 + 10.0 * i;
            pool.submit(() -> {
                try {
                    start.await();
                    processor.placeBid(a, b, amount);
                } catch (InvalidBidException expected) {
                    // bị từ chối vì đã có giá cao hơn: hợp lệ trong cuộc đua
                } catch (Exception e) {
                    synchronized (unexpected) {
                        unexpected.add(e);
                    }
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertTrue(unexpected.isEmpty(), "Lỗi ngoài dự kiến: " + unexpected);
        // Bid cao nhất luôn phải thắng
        assertEquals(100 + 10.0 * (bidders - 1), a.getCurrentPrice());
        // Lịch sử luôn tăng dần ít nhất bằng bước giá
        List<Bid> history = a.getBidHistory();
        for (int i = 1; i < history.size(); i++) {
            assertTrue(history.get(i).getBidAmount() - history.get(i - 1).getBidAmount() >= 10,
                    "Lịch sử giá không hợp lệ tại vị trí " + i);
        }
        assertEquals(history.get(history.size() - 1).getBidAmount(), a.getCurrentPrice());
    }
}
