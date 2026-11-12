package com.auction.server.model;

import java.time.Instant;

public class Bid extends Entity {
    private static final long serialVersionUID = 1L;

    private String auctionId;
    private String bidderId;
    private String bidderName;
    private double amount;

    public Bid() {
        super();
    }

    public Bid(String auctionId, String bidderId, String bidderName, double amount) {
        super();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getAmount() {
        return amount;
    }

    public Instant getTimestamp() {
        return getCreatedAt();
    }
}
