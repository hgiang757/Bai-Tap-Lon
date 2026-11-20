package com.auction.server.model;

/** Đại diện cho sản phẩm điện tử cùng thông tin bảo hành. */
public class Electronics extends Item {
  private static final long serialVersionUID = 1L;
  private int warrantyMonths;

  /** Khởi tạo thông tin cơ sở của sản phẩm điện tử. */
  public Electronics() {
    super();
    this.warrantyMonths = 12;
  }

  /** Khởi tạo thông tin cơ sở của sản phẩm điện tử. */
  public Electronics(String id, String name, double basePrice) {
    this(id, name, "", basePrice, "", 12);
  }

  /** Khởi tạo thông tin cơ sở của sản phẩm điện tử. */
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

  /** Đổi số tháng bảo hành và ghi nhận thời điểm cập nhật. */
  public void setWarrantyMonths(int warrantyMonths) {
    this.warrantyMonths = warrantyMonths;
    markAsUpdated();
  }
}
