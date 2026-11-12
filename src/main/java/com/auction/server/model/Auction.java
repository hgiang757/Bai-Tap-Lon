package com.auction.server.model;

import com.auction.server.exception.ApiException;

import java.time.LocalDateTime;

/**
 * Immutable auction rules for one snapshot. The service owns wallets, persistence and locking;
 * this value is rebuilt inside each transaction so it cannot outlive a rollback.
 */
public record Auction(
        String id,
        String sellerId,
        long currentPrice,
        long minBidStep,
        AuctionStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String highestBidderId,
        long totalBids,
        boolean settled) {

    /** Convenience constructor for a snapshot without bids. */
    public Auction(String id, String sellerId, long currentPrice, long minBidStep,
            String status, LocalDateTime startTime, LocalDateTime endTime) {
        this(id, sellerId, currentPrice, minBidStep, AuctionStatus.valueOf(status),
                startTime, endTime, "", 0, !AuctionStatus.valueOf(status).isActive());
    }

    public long minimumNextBid() {
        return currentPrice + minBidStep;
    }

    /** Server time and authenticated identity decide eligibility, never the client UI. */
    public void requireBidder(String userId, String role, LocalDateTime time) {
        if ("ADMIN".equals(role) || sellerId.equals(userId))
            throw new ApiException("FORBIDDEN", "Cannot bid on this product");
        if (startTime.isAfter(time))
            throw new ApiException("AUCTION_NOT_STARTED", "Auction has not started");
        if (status != AuctionStatus.RUNNING || !time.isBefore(endTime))
            throw new ApiException("AUCTION_CLOSED", "Auction is closed");
    }

    /** Amount format/limits and wallet availability are validated by the service. */
    public void requireMinimumBid(long amount) {
        if (amount < minimumNextBid())
            throw new ApiException(
                    "BID_TOO_LOW", "Bid must reach current price plus minimum step");
    }

    public void requireAutoBid(long maximum, long increment) {
        if (increment < minBidStep || maximum < minimumNextBid())
            throw new ApiException("VALIDATION_ERROR", "Invalid auto-bid step or maximum");
    }

    public void requireEditable() {
        if (status != AuctionStatus.OPEN || totalBids != 0)
            throw new ApiException("CONFLICT", "Only upcoming auctions without bids are editable");
    }

    public void requireCancelable() {
        if (!status.isActive())
            throw new ApiException("CONFLICT", "Auction cannot be canceled");
    }

    public boolean isLeading(String userId) {
        return !highestBidderId.isEmpty() && highestBidderId.equals(userId);
    }

    public long heldBy(String userId) {
        return !settled && isLeading(userId) ? currentPrice : 0;
    }

    public boolean hasExpired(LocalDateTime time) {
        return status.isActive() && !time.isBefore(endTime);
    }

    public boolean shouldStart(LocalDateTime time) {
        return status == AuctionStatus.OPEN && !time.isBefore(startTime);
    }

    /** The last accepted valid bid determines the sole leader; the old value stays immutable. */
    public Auction withBid(String bidderId, long amount) {
        requireMinimumBid(amount);
        return new Auction(id, sellerId, amount, minBidStep, status, startTime, endTime,
                bidderId, totalBids + 1, settled);
    }
}
