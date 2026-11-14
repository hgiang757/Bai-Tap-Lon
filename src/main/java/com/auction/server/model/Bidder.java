package com.auction.server.model;

/** Represents an auction participant in the server user hierarchy. */
public class Bidder extends User {
  private static final long serialVersionUID = 1L;
  private int wonAuctionsCount;

  /** Initializes the shared fields of this bidder domain object. */
  public Bidder() {
    super();
  }

  /** Initializes the shared fields of this bidder domain object. */
  public Bidder(String id, String name, String passwordHash) {
    super(id, name, passwordHash);
  }

  /** Initializes the shared fields of this bidder domain object. */
  public Bidder(String id, String name, String passwordHash, String email, double balance) {
    super(id, name, passwordHash, email, balance);
  }

  @Override
  public String getRoleName() {
    return "BIDDER";
  }

  public int getWonAuctionsCount() {
    return wonAuctionsCount;
  }

  /** Increments the legacy won-auction counter and updates the timestamp. */
  public void incrementWonAuctions() {
    this.wonAuctionsCount++;
    markAsUpdated();
  }
}
