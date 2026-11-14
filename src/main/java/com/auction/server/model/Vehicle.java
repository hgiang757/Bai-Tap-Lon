package com.auction.server.model;

/** Represents a vehicle item with mileage information. */
public class Vehicle extends Item {
  private static final long serialVersionUID = 1L;
  private int mileage;

  /** Initializes the shared fields of this vehicle domain object. */
  public Vehicle() {
    super();
    this.mileage = 0;
  }

  /** Initializes the shared fields of this vehicle domain object. */
  public Vehicle(String id, String name, double basePrice) {
    this(id, name, "", basePrice, "", 0);
  }

  /** Initializes the shared fields of this vehicle domain object. */
  public Vehicle(
      String id, String name, String description, double basePrice, String imageUrl, int mileage) {
    super(id, name, description, basePrice, imageUrl);
    this.mileage = mileage;
  }

  @Override
  public String getCategory() {
    return "VEHICLE";
  }

  public int getMileage() {
    return mileage;
  }

  /** Updates mileage and records the entity modification time. */
  public void setMileage(int mileage) {
    this.mileage = mileage;
    markAsUpdated();
  }
}
