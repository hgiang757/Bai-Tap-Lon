package com.auction.server.model;

public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;

    private String licensePlate;
    private int mileage;

    public Vehicle(String id, String name, String description, Seller seller, String licensePlate, int mileage) {
        super(id, name, description, seller);
        this.licensePlate = licensePlate;
        this.mileage = mileage;
    }

    @Override
    public double calculateInsuranceFee(double currentPrice) {
        // Phí bảo hiểm phương tiện: 3% giá trị hiện tại
        return currentPrice * 0.03;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public int getMileage() {
        return mileage;
    }
}
