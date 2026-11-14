package com.auction.server.model;

/** Represents a seller with rating information. */
public class Seller extends User {
  private static final long serialVersionUID = 1L;
  private double rating = 5.0;

  /** Initializes the shared fields of this seller domain object. */
  public Seller() {
    super();
  }

  /** Initializes the shared fields of this seller domain object. */
  public Seller(String id, String name, String passwordHash) {
    super(id, name, passwordHash);
  }

  /** Initializes the shared fields of this seller domain object. */
  public Seller(String id, String name, String passwordHash, String email, double balance) {
    super(id, name, passwordHash, email, balance);
  }

  @Override
  public String getRoleName() {
    return "SELLER";
  }

  public double getRating() {
    return rating;
  }

  /** Clamps the legacy seller rating to the range from zero to five. */
  public void setRating(double rating) {
    this.rating = Math.max(0.0, Math.min(5.0, rating));
    markAsUpdated();
  }
}
