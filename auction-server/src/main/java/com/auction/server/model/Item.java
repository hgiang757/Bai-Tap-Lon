package com.auction.server.model;

import java.time.Instant;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    protected String description;
    protected double basePrice;
    protected String sellerId;

    public Item() {
        super();
    }

    // Constructor 3 tham số (String id, String name, double basePrice)
    public Item(String id, String name, double basePrice) {
        super(id, name);
        this.basePrice = basePrice;
    }

    // Constructor 3 tham số (Long id, String name, double basePrice)
    public Item(Long id, String name, double basePrice) {
        super(id, name);
        this.basePrice = basePrice;
    }

    // Constructor 4 tham số (String id, String name, String description, double basePrice)
    public Item(String id, String name, String description, double basePrice) {
        super(id, name);
        this.description = description;
        this.basePrice = basePrice;
    }

    // Constructor 4 tham số (Long id, String name, String description, double basePrice)
    public Item(Long id, String name, String description, double basePrice) {
        super(id, name);
        this.description = description;
        this.basePrice = basePrice;
    }

    // Constructor 5 tham số (String id, ...)
    public Item(String id, String name, String description, double basePrice, String sellerId) {
        super(id, name);
        this.description = description;
        this.basePrice = basePrice;
        this.sellerId = sellerId;
    }

    // Constructor 5 tham số (Long id, ...)
    public Item(Long id, String name, String description, double basePrice, Long sellerId) {
        super(id, name);
        this.description = description;
        this.basePrice = basePrice;
        this.sellerId = sellerId != null ? String.valueOf(sellerId) : null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        setUpdatedAt(Instant.now());
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
        setUpdatedAt(Instant.now());
    }

    public double getStartingPrice() {
        return basePrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.basePrice = startingPrice;
        setUpdatedAt(Instant.now());
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
        setUpdatedAt(Instant.now());
    }

    // Các phương thức cho lớp con / anonymous class trong test override
    public String getCategory() {
        return "GENERAL";
    }

    public String getItemType() {
        return getCategory();
    }

    public String getDetails() {
        return description != null ? description : "";
    }

    public String getItemDetails() {
        return getDetails();
    }

    @Override
    public String toString() {
        return "Item{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", basePrice=" + basePrice +
                '}';
    }
}
