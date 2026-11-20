package com.auction.server.model;

/** Đại diện cho tài khoản tham gia đặt giá. */
public class Bidder extends User {
  private static final long serialVersionUID = 1L;
  private int wonAuctionsCount;

  /** Khởi tạo thông tin cơ sở của tài khoản đặt giá. */
  public Bidder() {
    super();
  }

  /** Khởi tạo thông tin cơ sở của tài khoản đặt giá. */
  public Bidder(String id, String name, String passwordHash) {
    super(id, name, passwordHash);
  }

  /** Khởi tạo thông tin cơ sở của tài khoản đặt giá. */
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

  /** Tăng bộ đếm số phiên đã thắng và ghi nhận thời điểm cập nhật. */
  public void incrementWonAuctions() {
    this.wonAuctionsCount++;
    markAsUpdated();
  }
}
