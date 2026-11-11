package com.auction.server.model;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin() {
        super();
    }

    public Admin(String id, String username, String passwordHash) {
        super(id, username, passwordHash);
    }

    public Admin(String id, String username, String passwordHash, String email) {
        super(id, username, passwordHash, email, 0.0);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }
}
