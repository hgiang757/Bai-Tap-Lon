package com.auction.server.model;

public final class Electronics extends Item {
    public Electronics(String id, String name, String description, Seller seller) {
        super(id, name, description, seller);
    }

    @Override
    public String getCategory() {
        return "ELECTRONICS";
    }
}
