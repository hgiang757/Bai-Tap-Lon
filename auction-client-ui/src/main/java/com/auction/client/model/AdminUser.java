package com.auction.client.model;

import java.time.LocalDateTime;

/** DTO tạm cho quản trị người dùng; TODO thay bằng model auction-common khi có. */
public class AdminUser {
    private final User user;
    private final LocalDateTime createdAt;
    private boolean locked;
    /** Tạo bản ghi quản trị. */
    public AdminUser(User user, LocalDateTime createdAt) {
        this.user = user;
        this.createdAt = createdAt;
    }
    /** Lấy người dùng. */
    public User getUser() { return user; }
    /** Lấy thời điểm tạo. */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** Kiểm tra trạng thái khóa. */
    public boolean isLocked() { return locked; }
    /** Đặt trạng thái khóa. */
    public void setLocked(boolean value) { locked = value; }
}
