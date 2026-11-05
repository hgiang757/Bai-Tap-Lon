package com.auction.server.model;

public final class Seller extends User {
    public Seller(String id, String name, String passwordHash) {
        super(id, name, passwordHash);
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }
}
