package com.auction.client.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entity đại diện cho một phiên đấu giá.
 */
public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String id;
    private Item item;
    private String sellerUsername;
    private double currentPrice;
    private String highestBidderUsername;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double minBidStep;
    private int totalBids;

    public Auction() {
    }

    public Auction(String id, Item item, String sellerUsername, double currentPrice,
                   String highestBidderUsername, AuctionStatus status,
                   LocalDateTime startTime, LocalDateTime endTime, double minBidStep) {
        this.id = id;
        this.item = item;
        this.sellerUsername = sellerUsername;
        this.currentPrice = currentPrice;
        this.highestBidderUsername = highestBidderUsername;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minBidStep = minBidStep;
        this.totalBids = 0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getItemName() {
        return item != null ? item.getName() : "";
    }

    public String getItemCategory() {
        return item != null ? item.getCategory() : "";
    }

    public double getStartingPrice() {
        return item != null ? item.getStartingPrice() : 0.0;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getHighestBidderUsername() {
        return highestBidderUsername != null ? highestBidderUsername : "Chưa có";
    }

    public void setHighestBidderUsername(String highestBidderUsername) {
        this.highestBidderUsername = highestBidderUsername;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getFormattedEndTime() {
        return endTime != null ? endTime.format(TIME_FORMATTER) : "Không xác định";
    }

    public double getMinBidStep() {
        return minBidStep;
    }

    public void setMinBidStep(double minBidStep) {
        this.minBidStep = minBidStep;
    }

    public int getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(int totalBids) {
        this.totalBids = totalBids;
    }

    public double getMinimumNextBid() {
        return currentPrice + minBidStep;
    }

    public String getFormattedCurrentPrice() {
        return com.auction.client.util.FormatUtil.formatCurrency(currentPrice);
    }

    public String getFormattedStartingPrice() {
        return com.auction.client.util.FormatUtil.formatCurrency(getStartingPrice());
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id='" + id + '\'' +
                ", item=" + (item != null ? item.getName() : "null") +
                ", currentPrice=" + currentPrice +
                ", status=" + status +
                '}';
    }
}
