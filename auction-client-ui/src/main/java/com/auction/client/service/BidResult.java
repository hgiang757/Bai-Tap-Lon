package com.auction.client.service;

import com.auction.client.model.Bid;

/**
 * Lớp kết quả phản hồi khi thực hiện đặt giá (Bid).
 */
public class BidResult {
    private final boolean success;
    private final String message;
    private final double updatedPrice;
    private final Bid newBid;

    public BidResult(boolean success, String message, double updatedPrice, Bid newBid) {
        this.success = success;
        this.message = message;
        this.updatedPrice = updatedPrice;
        this.newBid = newBid;
    }

    public static BidResult ok(String message, double updatedPrice, Bid bid) {
        return new BidResult(true, message, updatedPrice, bid);
    }

    public static BidResult fail(String message) {
        return new BidResult(false, message, 0.0, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public double getUpdatedPrice() {
        return updatedPrice;
    }

    public Bid getNewBid() {
        return newBid;
    }
}
