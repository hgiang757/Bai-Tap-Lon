package com.auction.server.model;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin() {
        super();
    }

    public Admin(String id, String name, String passwordHash) {
        super(id, name, passwordHash);
    }

    public Admin(String id, String name, String passwordHash, String email) {
        super(id, name, passwordHash, email, 0.0);
    }

    @Override
    public String getRoleName() {
        return "ADMIN";
    }
}
