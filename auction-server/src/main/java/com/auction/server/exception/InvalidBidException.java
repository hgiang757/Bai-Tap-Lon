package com.auction.server.exception;

/**
 * Ném ra khi đặt giá không hợp lệ (giá thấp hơn mức tối thiểu, giá không dương, đang dẫn đầu, cấu hình auto-bid sai...).
 */
public class InvalidBidException extends Exception {

    public InvalidBidException(String message) {
        super(message);
    }

    public InvalidBidException(String message, Throwable cause) {
        super(message, cause);
    }
}
