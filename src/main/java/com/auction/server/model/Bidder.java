package com.auction.server.model;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    private int wonAuctionsCount;

    public Bidder() {
        super();
    }

    public Bidder(String id, String name, String passwordHash) {
        super(id, name, passwordHash);
    }

    public Bidder(String id, String name, String passwordHash, String email, double balance) {
        super(id, name, passwordHash, email, balance);
    }

    @Override
    public String getRoleName() {
        return "BIDDER";
    }

    public int getWonAuctionsCount() {
        return wonAuctionsCount;
    }

    public void incrementWonAuctions() {
        this.wonAuctionsCount++;
        markAsUpdated();
    }
}
