package com.auction.server.model;

/** Đại diện cho phương tiện cùng quãng đường đã đi. */
public class Vehicle extends Item {
  private static final long serialVersionUID = 1L;
  private int mileage;

  /** Khởi tạo thông tin cơ sở của phương tiện. */
  public Vehicle() {
    super();
    this.mileage = 0;
  }

  /** Khởi tạo thông tin cơ sở của phương tiện. */
  public Vehicle(String id, String name, double basePrice) {
    this(id, name, "", basePrice, "", 0);
  }

  /** Khởi tạo thông tin cơ sở của phương tiện. */
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

  /** Đổi quãng đường đã đi và ghi nhận thời điểm cập nhật. */
  public void setMileage(int mileage) {
    this.mileage = mileage;
    markAsUpdated();
  }
}
