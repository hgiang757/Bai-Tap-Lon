package com.auction.client.service;

/** Lỗi thao tác ví mô phỏng. */
public class WalletException extends RuntimeException {
    /** Tạo lỗi với thông báo tiếng Việt. */
    public WalletException(String message) { super(message); }
}
