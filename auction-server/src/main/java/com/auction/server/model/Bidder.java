package com.auction.server.model;

// Lớp Người Mua (Đấu giá viên)
public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getRoleName() {
        return "BIDDER";
    }
}