package com.auction.client.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho sản phẩm được đưa lên sàn đấu giá.
 */
public class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private String category;
    private double startingPrice;
    private String sellerUsername;
    private LocalDateTime createdAt;

    public Item() {
    }

    public Item(String id, String name, String description, String category, double startingPrice, String sellerUsername) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.sellerUsername = sellerUsername;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return ItemCategory.from(category).getLabel();
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name + " (" + category + ") - " + com.auction.client.util.FormatUtil.formatCurrency(startingPrice);
    }
}
