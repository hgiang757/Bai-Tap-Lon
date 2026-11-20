package com.auction.server.model;

/** Đại diện cho tài khoản người bán cùng điểm đánh giá. */
public class Seller extends User {
  private static final long serialVersionUID = 1L;
  private double rating = 5.0;

  /** Khởi tạo thông tin cơ sở của tài khoản người bán. */
  public Seller() {
    super();
  }

  /** Khởi tạo thông tin cơ sở của tài khoản người bán. */
  public Seller(String id, String name, String passwordHash) {
    super(id, name, passwordHash);
  }

  /** Khởi tạo thông tin cơ sở của tài khoản người bán. */
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

  /** Giới hạn điểm đánh giá người bán từ 0 đến 5. */
  public void setRating(double rating) {
    this.rating = Math.max(0.0, Math.min(5.0, rating));
    markAsUpdated();
  }
}
