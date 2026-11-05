package com.auction.server.model;

public final class Vehicle extends Item {
    public Vehicle(String id, String name, String description, Seller seller) {
        super(id, name, description, seller);
    }

    @Override
    public String getCategory() {
        return "VEHICLE";
    }
}
