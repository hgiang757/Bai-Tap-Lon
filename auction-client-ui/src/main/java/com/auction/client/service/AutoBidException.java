package com.auction.client.service;

/** Lỗi cấu hình đặt giá tự động. */
public class AutoBidException extends RuntimeException {
    /** Tạo lỗi với thông báo tiếng Việt. */
    public AutoBidException(String message) { super(message); }
}
