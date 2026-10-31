package com.auction.server.model;

// Lớp Quản trị viên
public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getRoleName() {
        return "ADMIN";
    }
}