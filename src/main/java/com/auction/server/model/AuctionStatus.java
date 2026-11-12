package com.auction.server.model;

public enum AuctionStatus {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED;

    public boolean isActive() {
        return this == OPEN || this == RUNNING;
    }
}
