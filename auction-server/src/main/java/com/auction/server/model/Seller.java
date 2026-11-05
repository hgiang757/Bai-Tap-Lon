package com.auction.server.model;

public class Seller extends User {
    private static final long serialVersionUID = 1L;

    public Seller() {
        super();
    }

    public Seller(String id, String name, String password) {
        super(id, name, password);
    }

    public Seller(Long id, String name, String password) {
        super(id, name, password);
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }
}
