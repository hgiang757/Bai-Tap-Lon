package com.auction.server.model;

/** Đại diện cho tác phẩm nghệ thuật cùng thông tin tác giả. */
public class Art extends Item {
  private static final long serialVersionUID = 1L;
  private String artistName;

  /** Khởi tạo thông tin cơ sở của tác phẩm. */
  public Art() {
    super();
    this.artistName = "Unknown Artist";
  }

  /** Khởi tạo thông tin cơ sở của tác phẩm. */
  public Art(String id, String name, double basePrice) {
    this(id, name, "", basePrice, "", "Unknown Artist");
  }

  /** Khởi tạo thông tin cơ sở của tác phẩm. */
  public Art(
      String id,
      String name,
      String description,
      double basePrice,
      String imageUrl,
      String artistName) {
    super(id, name, description, basePrice, imageUrl);
    this.artistName = artistName;
  }

  @Override
  public String getCategory() {
    return "ART";
  }

  public String getArtistName() {
    return artistName;
  }

  /** Đổi tên tác giả và ghi nhận thời điểm cập nhật. */
  public void setArtistName(String artistName) {
    this.artistName = artistName;
    markAsUpdated();
  }
}
