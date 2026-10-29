package com.auction.client.model;

/**
 * Trạng thái của phiên đấu giá.
 */
public enum AuctionStatus {
    OPEN("Sắp diễn ra"),
    RUNNING("Đang diễn ra"),
    FINISHED("Đã kết thúc"),
    PAID("Đã thanh toán"),
    CANCELED("Đã hủy");

    private final String description;

    AuctionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
