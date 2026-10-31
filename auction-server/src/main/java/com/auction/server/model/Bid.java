package com.auction.server.model;

import java.io.Serializable;
import java.time.LocalDateTime;

// Thể hiện một giao dịch trả giá cụ thể
public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;

    private String bidId;
    private Bidder bidder; // Ai là người đặt giá? (Aggregation)
    private double bidAmount;
    private LocalDateTime timestamp;

    public Bid(String bidId, Bidder bidder, double bidAmount) {
        this.bidId = bidId;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}