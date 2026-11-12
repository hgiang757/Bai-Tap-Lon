package com.auction.server.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Alternate model retained from main; authoritative transactions use AuctionService and Auction. */
public class AuctionDraft extends Entity {
    private static final long serialVersionUID = 1L;

    private Item item;
    private String sellerId;
    private Instant startTime;
    private Instant endTime;
    private double minIncrement;
    private double currentPrice;
    private AuctionStatus status;
    private final List<Bid> bidHistory;
    private Bid highestBid;

    public AuctionDraft(
            String id,
            Item item,
            String sellerId,
            Instant startTime,
            Instant endTime,
            double minIncrement) {
        super(id, item != null ? item.getName() : "Auction");
        this.item = item;
        this.sellerId = sellerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minIncrement = minIncrement > 0 ? minIncrement : 1.0;
        this.currentPrice = item != null ? item.getBasePrice() : 0.0;
        this.status = AuctionStatus.RUNNING;
        this.bidHistory = new ArrayList<>();
    }

    public boolean isRunning(Instant now) {
        return status == AuctionStatus.RUNNING
                && (startTime == null || !now.isBefore(startTime))
                && (endTime == null || now.isBefore(endTime));
    }

    public double getMinimumNextBid() {
        if (highestBid == null) {
            return Math.max(currentPrice, item.getBasePrice());
        }
        return currentPrice + minIncrement;
    }

    public void recordBid(Bid bid) {
        this.bidHistory.add(bid);
        this.highestBid = bid;
        this.currentPrice = bid.getAmount();
        markAsUpdated();
    }

    public void extendEndTime(long extraSeconds) {
        if (this.endTime != null && extraSeconds > 0) {
            this.endTime = this.endTime.plusSeconds(extraSeconds);
            markAsUpdated();
        }
    }

    public String getWinnerId() {
        return highestBid != null ? highestBid.getBidderId() : null;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
        markAsUpdated();
    }

    public String getSellerId() {
        return sellerId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
        markAsUpdated();
    }

    public double getMinIncrement() {
        return minIncrement;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
        markAsUpdated();
    }

    public List<Bid> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    public Bid getHighestBid() {
        return highestBid;
    }
}
