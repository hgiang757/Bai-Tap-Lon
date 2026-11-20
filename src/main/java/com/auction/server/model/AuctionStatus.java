package com.auction.server.model;

/** Liệt kê các trạng thái từ lúc chờ mở đến khi phiên đấu giá kết thúc. */
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
