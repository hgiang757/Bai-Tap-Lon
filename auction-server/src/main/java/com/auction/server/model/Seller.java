package com.auction.server.model;

// Lớp Người Bán
public class Seller extends User {
    private static final long serialVersionUID = 1L;

    public Seller(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }
}