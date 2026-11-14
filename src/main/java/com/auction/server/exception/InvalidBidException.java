package com.auction.server.exception;

/** Rejects a bid below the current minimum without changing the protocol error code. */
public final class InvalidBidException extends ApiException {
  /** Creates a minimum-price rejection. */
  public InvalidBidException(String message) {
    super("BID_TOO_LOW", message);
  }
}
