package com.auction.server.model;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    private int wonAuctionsCount;

    public Bidder() {
        super();
        this.wonAuctionsCount = 0;
    }

    public Bidder(String username, String password, String email) {
        super(username, password, email);
        this.wonAuctionsCount = 0;
    }

    public Bidder(Long id, String username, String password, String email, double balance) {
        super(id, username, password, email, balance);
        this.wonAuctionsCount = 0;
    }

    @Override
    public UserRole getRole() {
        return UserRole.BIDDER;
    }

    public int getWonAuctionsCount() {
        return wonAuctionsCount;
    }

    public void setWonAuctionsCount(int wonAuctionsCount) {
        this.wonAuctionsCount = wonAuctionsCount;
        markAsUpdated();
    }

    public void incrementWonAuctions() {
        this.wonAuctionsCount++;
        markAsUpdated();
    }

    @Override
    public String toString() {
        return "Bidder{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", balance=" + getBalance() +
                ", wonAuctionsCount=" + wonAuctionsCount +
                '}';
    }
}
