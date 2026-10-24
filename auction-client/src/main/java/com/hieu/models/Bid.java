package com.hieu.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entity đại diện cho một lượt đặt giá (Bid) của người tham gia.
 */
public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private String id;
    private String auctionId;
    private String bidderUsername;
    private double amount;
    private LocalDateTime timestamp;

    public Bid() {
    }

    public Bid(String id, String auctionId, String bidderUsername, double amount, LocalDateTime timestamp) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public void setBidderUsername(String bidderUsername) {
        this.bidderUsername = bidderUsername;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTime() {
        return timestamp != null ? timestamp.format(FORMATTER) : "";
    }

    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] " + bidderUsername + ": " + com.hieu.util.FormatUtil.formatCurrency(amount);
    }
}
