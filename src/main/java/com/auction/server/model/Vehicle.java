package com.auction.server.model;

public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;
    private int mileage;

    public Vehicle() {
        super();
    }

    public Vehicle(String id, String name, double basePrice) {
        this(id, name, "", basePrice, "", 0);
    }

    public Vehicle(String id, String name, String description, double basePrice, String imageUrl, int mileage) {
        super(id, name, description, basePrice, imageUrl);
        this.mileage = mileage;
    }

    @Override
    public String getCategory() {
        return "VEHICLE";
    }

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
        markAsUpdated();
    }
}
