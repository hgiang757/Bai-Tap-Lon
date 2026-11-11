package com.auction.server.model;

public class Seller extends User {
    private static final long serialVersionUID = 1L;

    private double rating;

    public Seller() {
        super();
        this.rating = 5.0;
    }

    public Seller(String username, String password, String email) {
        super(username, password, email);
        this.rating = 5.0;
    }

    public Seller(Long id, String username, String password, String email, double balance, double rating) {
        super(id, username, password, email, balance);
        this.rating = rating;
    }

    @Override
    public UserRole getRole() {
        return UserRole.SELLER;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        if (rating < 0.0 || rating > 5.0) {
            throw new IllegalArgumentException("Rating phải nằm trong khoảng từ 0.0 đến 5.0");
        }
        this.rating = rating;
        markAsUpdated();
    }

    @Override
    public String toString() {
        return "Seller{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", balance=" + getBalance() +
                ", rating=" + rating +
                '}';
    }
}
