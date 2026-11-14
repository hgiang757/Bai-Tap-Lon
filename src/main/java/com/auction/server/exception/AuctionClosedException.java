package com.auction.server.exception;

/** Rejects participation in an auction that no longer accepts bids. */
public final class AuctionClosedException extends ApiException {
  /** Creates an auction-closed rejection. */
  public AuctionClosedException(String message) {
    super("AUCTION_CLOSED", message);
  }
}
