package com.auction.server.model;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private String description;
    private double basePrice;
    private String imageUrl;
    private Seller seller;

    public Item() {
        super();
    }

    public Item(String id, String name, double basePrice) {
        this(id, name, "", basePrice, "");
    }

    public Item(String id, String name, String description, double basePrice, String imageUrl) {
        super(id, name);
        this.description = description;
        this.basePrice = basePrice;
        this.imageUrl = imageUrl;
    }

    public abstract String getCategory();

    // Khớp với lời gọi chuỗi: ItemFactory.forCategory(category).create(id, name, description, seller)
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

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
        markAsUpdated();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        markAsUpdated();
    }
}
