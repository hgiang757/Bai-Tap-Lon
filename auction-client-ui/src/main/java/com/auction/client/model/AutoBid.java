package com.auction.client.model;

import java.time.LocalDateTime;

/** Thiết lập đặt giá tự động của một người trong một phiên. */
public final class AutoBid {
    private final String id, auctionId, username;
    private final long maxBid, increment;
    private final LocalDateTime createdAt;
    private boolean active;
    private boolean limitReached;
    private int autoCount;
    /** Tạo thiết lập đặt giá tự động. */
    public AutoBid(String id, String auctionId, String username, long maxBid, long increment, LocalDateTime createdAt) {
        this.id=id; this.auctionId=auctionId; this.username=username; this.maxBid=maxBid;
        this.increment=increment; this.createdAt=createdAt; this.active=true;
    }
    /** Sao chép trạng thái để trả về cho giao diện. */
    public AutoBid copy() { AutoBid a = new AutoBid(id, auctionId, username, maxBid, increment, createdAt); a.active=active; a.limitReached=limitReached; a.autoCount=autoCount; return a; }
    /** Mã thiết lập. */ public String getId() { return id; }
    /** Mã phiên. */ public String getAuctionId() { return auctionId; }
    /** Tên người dùng. */ public String getUsername() { return username; }
    /** Giá tối đa. */ public long getMaxBid() { return maxBid; }
    /** Bước giá. */ public long getIncrement() { return increment; }
    /** Thời điểm đăng ký. */ public LocalDateTime getCreatedAt() { return createdAt; }
    /** Trạng thái bật. */ public boolean isActive() { return active; }
    /** Số lượt tự đặt. */ public int getAutoCount() { return autoCount; }
    /** Dừng thiết lập. */ public void deactivate() { active=false; }
    /** Đánh dấu thiết lập đã chạm trần. */ public void reachLimit() { active=false; limitReached=true; }
    /** Thiết lập đã chạm trần. */ public boolean isLimitReached() { return limitReached; }
    /** Ghi nhận một lượt tự đặt. */ public void recordBid() { autoCount++; }
}
