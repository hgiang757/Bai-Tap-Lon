package com.auction.server.model;

import java.time.Instant;

public class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private double basePrice;
    private Long sellerId;

    public Item() {
        super();
    }

    public Item(Long id, String name, String description, double basePrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
    }

    public Item(Long id, String name, String description, double basePrice, Long sellerId) {
        super(id);
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.sellerId = sellerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setUpdatedAt(Instant.now());
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

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
        setUpdatedAt(Instant.now());
    }

    public String getCategory() {
        return "GENERAL";
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", basePrice=" + basePrice +
                ", category='" + getCategory() + '\'' +
                '}';
    }
}
