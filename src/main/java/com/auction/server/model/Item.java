package com.auction.server.model;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private String description;
    private double basePrice;
    private String imageUrl;

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
