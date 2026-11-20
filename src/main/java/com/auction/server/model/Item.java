package com.auction.server.model;

/** Định nghĩa thông tin chung và danh mục của sản phẩm đấu giá. */
public abstract class Item extends Entity {
  private static final long serialVersionUID = 1L;

  private String description;
  private double basePrice;
  private String imageUrl;
  private Seller seller;

  /** Khởi tạo thông tin cơ sở của sản phẩm. */
  public Item() {
    super();
  }

  /** Khởi tạo thông tin cơ sở của sản phẩm. */
  public Item(String id, String name, double basePrice) {
    this(id, name, "", basePrice, "");
  }

  /** Khởi tạo thông tin cơ sở của sản phẩm. */
  public Item(String id, String name, String description, double basePrice, String imageUrl) {
    super(id, name);
    this.description = description;
    this.basePrice = basePrice;
    this.imageUrl = imageUrl;
  }

  /** Trả về danh mục do lớp sản phẩm cụ thể xác định. */
  public abstract String getCategory();

  // Khớp với lời gọi chuỗi: ItemFactory.forCategory(category).create(id, name, description, seller)
  /** Gán mã, tên, mô tả và người bán cho sản phẩm. */
  public Item create(String id, String name, String description, Seller seller) {
    setId(id);
    setName(name);
    setDescription(description);
    setSeller(seller);
    return this;
  }

  public Seller getSeller() {
    return seller;
  }

  /** Đổi người bán và ghi nhận thời điểm cập nhật. */
  public void setSeller(Seller seller) {
    this.seller = seller;
    markAsUpdated();
  }

  public String getType() {
    return getCategory();
  }

  public String getItemType() {
    return getCategory();
  }

  public String getDescription() {
    return description;
  }

  /** Đổi mô tả và ghi nhận thời điểm cập nhật. */
  public void setDescription(String description) {
    this.description = description;
    markAsUpdated();
  }

  public double getBasePrice() {
    return basePrice;
  }

  public double getStartingPrice() {
    return basePrice;
  }

  /** Đổi giá khởi điểm và ghi nhận thời điểm cập nhật. */
  public void setBasePrice(double basePrice) {
    this.basePrice = basePrice;
    markAsUpdated();
  }

  public String getImageUrl() {
    return imageUrl;
  }

  /** Đổi địa chỉ ảnh và ghi nhận thời điểm cập nhật. */
  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
    markAsUpdated();
  }
}
