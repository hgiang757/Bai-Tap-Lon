package com.auction.server.model;

public final class Bidder extends User {
    public Bidder(String id, String name, String passwordHash) {
        super(id, name, passwordHash);
    }

    @Override
    public String getRoleName() {
        return "BIDDER";
    }
}
