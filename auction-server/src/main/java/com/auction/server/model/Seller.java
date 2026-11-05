package com.auction.server.model;

public class Seller extends User {
    private static final long serialVersionUID = 1L;

    public Seller() {
        super();
        setRole("SELLER");
    }

    public Seller(String id, String name) {
        super(id, name, "", "SELLER");
    }

    public Seller(Long id, String name) {
        super(id, name, "", "SELLER");
    }

    public Seller(String id, String name, String password) {
        super(id, name, password, "SELLER");
    }

    public Seller(Long id, String name, String password) {
        super(id, name, password, "SELLER");
    }

    public Seller(String id, String name, String password, String email) {
        super(id, name, password, email, "SELLER");
    }

    public Seller(Long id, String name, String password, String email) {
        super(id, name, password, email, "SELLER");
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }
}
