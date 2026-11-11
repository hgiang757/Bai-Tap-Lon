package com.auction.server.model;

public class Electronics extends Item {
    private static final long serialVersionUID = 1L;
    private int warrantyMonths;

    public Electronics() {
        super();
    }

    public Electronics(String id, String name, double basePrice) {
        this(id, name, "", basePrice, "", 12);
    }

    public Electronics(String id, String name, String description, double basePrice, String imageUrl, int warrantyMonths) {
        super(id, name, description, basePrice, imageUrl);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getCategory() {
        return "ELECTRONICS";
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
        markAsUpdated();
    }
}
