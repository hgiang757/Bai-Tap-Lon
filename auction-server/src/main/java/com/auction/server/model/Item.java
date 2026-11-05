package com.auction.server.model;

import java.time.Instant;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    protected String description;
    protected double basePrice;
    protected Seller seller;
    protected String sellerId;

    public Item() {
        super();
    }

    // Constructor khớp chính xác với ItemDAOTest và BidProcessorTest:
    // super(id, name, "mô tả", (Seller) null);
    public Item(String id, String name, String description, Seller seller) {
        super(id, name);
        this.description = description;
        this.seller = seller;
        this.sellerId = seller != null ? seller.getId() : null;
    }

    public Item(Long id, String name, String description, Seller seller) {
        super(id, name);
        this.description = description;
        this.seller = seller;
        this.sellerId = seller != null ? seller.getId() : null;
    }

    public Item(String id, String name, String description, double basePrice) {
        super(id, name);
        this.description = description;
        this.basePrice = basePrice;
    }

    public Item(Long id, String name, String description, double basePrice) {
        super(id, name);
        this.description = description;
        this.basePrice = basePrice;
    }

    public Item(String id, String name, String description, double basePrice, Seller seller) {
        super(id, name);
        this.description = description;
        this.basePrice = basePrice;
        this.seller = seller;
        this.sellerId = seller != null ? seller.getId() : null;
    }

    // Phương thức bắt buộc để @Override trong ItemDAOTest và BidProcessorTest
    public abstract double calculateInsuranceFee(double currentPrice);

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

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
        this.sellerId = seller != null ? seller.getId() : null;
        setUpdatedAt(Instant.now());
    }

    public String getSellerId() {
        return seller != null ? seller.getId() : sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
        setUpdatedAt(Instant.now());
    }
}
