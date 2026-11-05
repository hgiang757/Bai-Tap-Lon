package com.auction.server.model;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder() {
        super();
        setRole("BIDDER");
    }

    public Bidder(Long id, String name, String password) {
        super(id, name, password, "BIDDER");
    }

    public Bidder(Long id, String name, String password, String email) {
        super(id, name, password, email, "BIDDER");
    }
}
