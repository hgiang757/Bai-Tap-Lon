package com.auction.client.service;

/** Từ chối cấu hình trước khi ghi; lỗi kết quả chưa rõ phải dùng exception khác. */
public class AutoBidException extends RuntimeException {
  /** Tạo lỗi với thông báo tiếng Việt. */
  public AutoBidException(String message) {
    super(message);
  }
}
