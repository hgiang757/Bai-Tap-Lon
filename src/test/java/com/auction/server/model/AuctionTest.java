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
}
