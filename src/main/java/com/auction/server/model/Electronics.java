package com.auction.server.model;

/** Represents an electronics item with warranty information. */
public class Electronics extends Item {
  private static final long serialVersionUID = 1L;
  private int warrantyMonths;

  /** Initializes the shared fields of this electronics domain object. */
  public Electronics() {
    super();
    this.warrantyMonths = 12;
  }

  /** Initializes the shared fields of this electronics domain object. */
  public Electronics(String id, String name, double basePrice) {
    this(id, name, "", basePrice, "", 12);
  }

  /** Initializes the shared fields of this electronics domain object. */
  public Electronics(
      String id,
      String name,
      String description,
      double basePrice,
      String imageUrl,
      int warrantyMonths) {
    super(id, name, description, basePrice, imageUrl);
    this.warrantyMonths = warrantyMonths;
  }

  @Override
  public String getCategory() {
    return "ELECTRONICS";
  }

  public int getWarrantyMonths() {
    return warrantyMonths;
  }

  /** Updates warranty months and records the entity modification time. */
  public void setWarrantyMonths(int warrantyMonths) {
    this.warrantyMonths = warrantyMonths;
    markAsUpdated();
  }
}
