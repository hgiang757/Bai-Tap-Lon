package com.auction.server.model;

public class Seller extends User {
    private static final long serialVersionUID = 1L;
    private double rating = 5.0;

    public Seller() {
        super();
    }

    public Seller(String id, String name, String passwordHash) {
        super(id, name, passwordHash);
    }

    public Seller(String id, String name, String passwordHash, String email, double balance) {
        super(id, name, passwordHash, email, balance);
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = Math.max(0.0, Math.min(5.0, rating));
        markAsUpdated();
    }
}
