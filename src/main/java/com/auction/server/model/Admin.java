package com.auction.server.model;

public final class Admin extends User {
    public Admin(String id, String name, String passwordHash) {
        super(id, name, passwordHash);
    }

    @Override
    public String getRoleName() {
        return "ADMIN";
    }
}
