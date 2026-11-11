package com.auction.server.model;

public class Seller extends User {
    private static final long serialVersionUID = 1L;
    private double rating;

    public Seller() {
        super();
        this.rating = 5.0;
    }

    public Seller(String id, String username, String passwordHash) {
        super(id, username, passwordHash);
        this.rating = 5.0;
    }

    public Seller(String id, String username, String passwordHash, String email, double balance) {
        super(id, username, passwordHash, email, balance);
        this.rating = 5.0;
    }

    @Override
    public UserRole getRole() {
        return UserRole.SELLER;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = Math.max(0.0, Math.min(5.0, rating));
        markAsUpdated();
    }
}
