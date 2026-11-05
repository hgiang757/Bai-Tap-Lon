package com.auction.server.model;

public class Art extends Item {
    private static final long serialVersionUID = 1L;

    private String artist;
    private int yearCreated;

    public Art(String id, String name, String description, Seller seller, String artist, int yearCreated) {
        super(id, name, description, seller);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    @Override
    public double calculateInsuranceFee(double currentPrice) {
        // Phí bảo hiểm tác phẩm nghệ thuật: 5% giá trị hiện tại
        return currentPrice * 0.05;
    }

    public String getArtist() {
        return artist;
    }

    public int getYearCreated() {
        return yearCreated;
    }
}
