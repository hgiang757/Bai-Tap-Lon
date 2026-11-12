package com.auction.server.model;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.server.exception.ApiException;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuctionTest {
    private final LocalDateTime start = LocalDateTime.of(2026, 11, 12, 10, 0);

    private Auction auction(String status) {
        return new Auction("auction", "seller", 1_000_000, 100_000, status, start, start.plusHours(1));
    }

    private void rejects(String code, Runnable work) {
        assertEquals(code, assertThrows(ApiException.class, work::run).code());
    }

    @Test
    void minimumStepAppliesToFirstBidAndAllowsLargerJumps() {
        Auction auction = auction("RUNNING");
        assertEquals(1_100_000, auction.minimumNextBid());
        rejects("BID_TOO_LOW", () -> auction.requireMinimumBid(1_099_999));
        assertDoesNotThrow(() -> auction.requireMinimumBid(1_100_000));
        assertDoesNotThrow(() -> auction.requireMinimumBid(1_200_000));
    }

    @Test
    void participantRoleAndOwnershipAreEnforcedOnServer() {
        Auction auction = auction("RUNNING");
        rejects("FORBIDDEN", () -> auction.requireBidder("admin", "ADMIN", start));
        rejects("FORBIDDEN", () -> auction.requireBidder("seller", "SELLER", start));
        assertDoesNotThrow(() -> auction.requireBidder("buyer", "BIDDER", start));
        assertDoesNotThrow(() -> auction.requireBidder("another-seller", "SELLER", start));
    }

    @Test
    void startIsInclusiveAndEndIsExclusive() {
        Auction auction = auction("RUNNING");
        rejects("AUCTION_NOT_STARTED", () -> auction.requireBidder("buyer", "BIDDER", start.minusNanos(1)));
        assertDoesNotThrow(() -> auction.requireBidder("buyer", "BIDDER", start));
        assertDoesNotThrow(() -> auction.requireBidder("buyer", "BIDDER", start.plusHours(1).minusNanos(1)));
        rejects("AUCTION_CLOSED", () -> auction.requireBidder("buyer", "BIDDER", start.plusHours(1)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "FINISHED", "PAID", "CANCELED"})
    void onlyRunningStatusAcceptsBids(String status) {
        rejects("AUCTION_CLOSED", () -> auction(status).requireBidder("buyer", "BIDDER", start));
    }

    @Test
    void acceptedBidChangesOneLeaderWithoutMutatingThePreviousSnapshot() {
        Auction previous = auction("RUNNING");
        Auction first = previous.withBid("buyer", 1_100_001);
        Auction second = first.withBid("rival", 1_200_001);
        assertEquals(1_000_000, previous.currentPrice());
        assertEquals("", previous.highestBidderId());
        assertEquals(0, previous.totalBids());
        assertTrue(first.isLeading("buyer"));
        assertFalse(second.isLeading("buyer"));
        assertTrue(second.isLeading("rival"));
        assertFalse(previous.isLeading(""));
        assertEquals(2, second.totalBids());
        assertEquals(1_200_001, second.heldBy("rival"));
        assertEquals(0, second.heldBy("buyer"));
        rejects("BID_TOO_LOW", () -> first.withBid("rival", 1_200_000));
    }

    @Test
    void settledAuctionsKeepTheirWinnerButReleaseTheHold() {
        Auction paid = new Auction("a", "seller", 9_000_000_000_001L, 1,
                AuctionStatus.PAID, start, start.plusHours(1), "winner", 3, true);
        assertTrue(paid.isLeading("winner"));
        assertEquals(0, paid.heldBy("winner"));
        assertEquals(9_000_000_000_002L, paid.minimumNextBid());
    }

    @Test
    void autoBidRequiresBothTheMinimumIncrementAndTheNextPrice() {
        Auction auction = auction("RUNNING");
        rejects("VALIDATION_ERROR", () -> auction.requireAutoBid(1_100_000, 99_999));
        rejects("VALIDATION_ERROR", () -> auction.requireAutoBid(1_099_999, 100_000));
        assertDoesNotThrow(() -> auction.requireAutoBid(1_100_000, 100_000));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED"})
    void editCancelAndSchedulerRulesRespectTypedStatus(String status) {
        Auction auction = auction(status);
        boolean active = status.equals("OPEN") || status.equals("RUNNING");
        assertEquals(active, auction.status().isActive());
        assertFalse(auction.hasExpired(start.plusHours(1).minusNanos(1)));
        assertEquals(active, auction.hasExpired(start.plusHours(1)));
        assertFalse(auction.shouldStart(start.minusNanos(1)));
        assertEquals(status.equals("OPEN"), auction.shouldStart(start));
        if (active) assertDoesNotThrow(auction::requireCancelable);
        else rejects("CONFLICT", auction::requireCancelable);
        if (status.equals("OPEN")) assertDoesNotThrow(auction::requireEditable);
        else rejects("CONFLICT", auction::requireEditable);
    }

    @Test
    void evenAnOpenAuctionCannotBeEditedOnceItHasBidHistory() {
        rejects("CONFLICT", () -> auction("OPEN").withBid("buyer", 1_100_000).requireEditable());
    }
}
