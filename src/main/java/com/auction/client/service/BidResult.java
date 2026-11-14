package com.auction.client.service;

import com.auction.client.model.Bid;

/** Lớp kết quả phản hồi khi thực hiện đặt giá (Bid). */
public class BidResult {
  private final boolean success;
  private final String message;
  private final double updatedPrice;
  private final Bid newBid;

  /** Stores the bid outcome, message and optional accepted bid. */
  public BidResult(boolean success, String message, double updatedPrice, Bid newBid) {
    this.success = success;
    this.message = message;
    this.updatedPrice = updatedPrice;
    this.newBid = newBid;
  }

  /** Creates a successful bid result containing the accepted bid. */
  public static BidResult ok(String message, double updatedPrice, Bid bid) {
    return new BidResult(true, message, updatedPrice, bid);
  }

  /** Creates a rejected bid result without an accepted bid. */
  public static BidResult fail(String message) {
    return new BidResult(false, message, 0.0, null);
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public double getUpdatedPrice() {
    return updatedPrice;
  }

  public Bid getNewBid() {
    return newBid;
  }
}
