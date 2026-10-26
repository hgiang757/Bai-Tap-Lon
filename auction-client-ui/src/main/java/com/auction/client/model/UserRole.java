package com.auction.client.model;

/**
 * Enum đại diện cho vai trò người dùng trong hệ thống đấu giá.
 */
public enum UserRole {
    BIDDER("Người đấu giá"),
    SELLER("Người bán");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
