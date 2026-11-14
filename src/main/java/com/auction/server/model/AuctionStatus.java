package com.auction.server.model;

/** Lists the scheduled, running and terminal auction states. */
public enum AuctionStatus {
  OPEN,
  RUNNING,
  FINISHED,
  PAID,
  CANCELED;

  public boolean isActive() {
    return this == OPEN || this == RUNNING;
  }
}
