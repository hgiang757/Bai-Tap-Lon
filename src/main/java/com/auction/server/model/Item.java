package com.auction.server.model;

/** Defines shared product data and the category contract for concrete item types. */
public abstract class Item extends Entity {
  private static final long serialVersionUID = 1L;

  private String description;
  private double basePrice;
  private String imageUrl;
  private Seller seller;

  /** Initializes the shared fields of this item domain object. */
  public Item() {
    super();
  }

  /** Initializes the shared fields of this item domain object. */
  public Item(String id, String name, double basePrice) {
    this(id, name, "", basePrice, "");
  }

  /** Initializes the shared fields of this item domain object. */
  public Item(String id, String name, String description, double basePrice, String imageUrl) {
    super(id, name);
    this.description = description;
    this.basePrice = basePrice;
    this.imageUrl = imageUrl;
  }

  /** Returns the category defined by the concrete item type. */
  public abstract String getCategory();

  // Khớp với lời gọi chuỗi: ItemFactory.forCategory(category).create(id, name, description, seller)
  /** Assigns product identity, description and owner to the concrete item. */
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

  /** Updates seller and records the entity modification time. */
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

  /** Updates description and records the entity modification time. */
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

  /** Updates base price and records the entity modification time. */
  public void setBasePrice(double basePrice) {
    this.basePrice = basePrice;
    markAsUpdated();
  }

  public String getImageUrl() {
    return imageUrl;
  }

  /** Updates image url and records the entity modification time. */
  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
    markAsUpdated();
  }
}
