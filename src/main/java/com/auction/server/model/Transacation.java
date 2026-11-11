package com.auction.server.model;

public class Transaction extends Entity {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String auctionId;
    private double amount;
    private String type; // DEPOSIT, WITHDRAW, BID_PAYMENT, SALE_PROCEEDS
    private String description;

    public Transaction(String userId, String auctionId, double amount, String type, String description) {
        super();
        this.userId = userId;
        this.auctionId = auctionId;
        this.amount = amount;
        this.type = type;
        this.description = description;
    }

    public String getUserId() {
        return userId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}
