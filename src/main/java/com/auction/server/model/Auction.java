package com.auction.server.model;

import com.auction.server.exception.ApiException;

import java.time.LocalDateTime;

/**
 * Immutable bidding rules for one snapshot. The service owns wallets, persistence and locking;
 * this value is rebuilt inside each transaction so it cannot outlive a rollback.
 */
public record Auction(
        String id,
        String sellerId,
        long currentPrice,
        long minBidStep,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime) {

    public long minimumNextBid() {
        return currentPrice + minBidStep;
    }

    /** Server time and authenticated identity decide eligibility, never the client UI. */
    public void requireBidder(String userId, String role, LocalDateTime time) {
        if ("ADMIN".equals(role) || sellerId.equals(userId))
            throw new ApiException("FORBIDDEN", "Cannot bid on this product");
        if (startTime.isAfter(time))
            throw new ApiException("AUCTION_NOT_STARTED", "Auction has not started");
        if (!"RUNNING".equals(status) || !time.isBefore(endTime))
            throw new ApiException("AUCTION_CLOSED", "Auction is closed");
    }

    /** Amount format/limits and wallet availability are validated by the service. */
    public void requireMinimumBid(long amount) {
        if (amount < minimumNextBid())
            throw new ApiException(
                    "BID_TOO_LOW", "Bid must reach current price plus minimum step");
    }
}
