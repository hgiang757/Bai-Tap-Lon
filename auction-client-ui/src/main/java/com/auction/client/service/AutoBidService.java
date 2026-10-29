package com.auction.client.service;

import com.auction.client.model.AutoBid;

/** Dịch vụ mô phỏng đặt giá tự động. */
public interface AutoBidService {
    /** Bật đặt giá tự động cho tài khoản hiện tại. */ AutoBid registerAutoBid(String auctionId, long maxBid, long increment);
    /** Tắt đặt giá tự động. */ void cancelAutoBid(String auctionId);
    /** Lấy cấu hình gần nhất của tài khoản hiện tại. */ AutoBid getMyAutoBid(String auctionId);
}
