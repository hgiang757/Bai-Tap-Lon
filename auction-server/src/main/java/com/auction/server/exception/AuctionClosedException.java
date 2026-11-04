package com.auction.server.exception;

/**
 * Ném ra khi đặt giá vào phiên đấu giá đã kết thúc hoặc đã hết giờ.
 */
public class AuctionClosedException extends Exception {

    public AuctionClosedException(String message) {
        super(message);
    }

    public AuctionClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
