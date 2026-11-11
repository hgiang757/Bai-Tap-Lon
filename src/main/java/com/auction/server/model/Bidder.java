package com.auction.server.model;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder() { super(); }
    public Bidder(String id, String name, String passwordHash) { super(id, name, passwordHash); }
    public Bidder(String id, String name, String passwordHash, double balance) { super(id, name, passwordHash, null, balance); }

    @Override
    public String getRoleName() {
        return "BIDDER";
    }
}
