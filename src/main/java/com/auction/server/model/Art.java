package com.auction.server.model;

public final class Art extends Item {
    public Art(String id, String name, String description, Seller seller) {
        super(id, name, description, seller);
    }

    @Override
    public String getCategory() {
        return "ART";
    }
}
