package com.auction.server.model;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder() {
        super();
    }

    public Bidder(String id, String name, String password) {
        super(id, name, password);
    }

    public Bidder(Long id, String name, String password) {
        super(id, name, password);
    }

    @Override
    public String getRoleName() {
        return "BIDDER";
    }
}
