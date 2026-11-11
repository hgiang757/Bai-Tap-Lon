package com.auction.server.model;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin() {
        super();
    }

    public Admin(String id, String name, String passwordHash) {
        super(id, name, passwordHash);
    }

    @Override
    public String getRoleName() {
        return "ADMIN";
    }
}
