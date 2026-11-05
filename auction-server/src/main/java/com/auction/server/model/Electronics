package com.auction.server.model;

public class Electronics extends Item {
    private static final long serialVersionUID = 1L;

    private String brand;
    private int warrantyMonths;

    public Electronics(String id, String name, String description, Seller seller, String brand, int warrantyMonths) {
        super(id, name, description, seller);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public double calculateInsuranceFee(double currentPrice) {
        // Phí bảo hiểm đồ điện tử: 2% giá trị hiện tại
        return currentPrice * 0.02;
    }

    public String getBrand() {
        return brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }
}
