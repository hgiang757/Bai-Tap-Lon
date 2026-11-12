package com.auction.server.model;

public class Art extends Item {
    private static final long serialVersionUID = 1L;
    private String artistName;

    public Art() {
        super();
        this.artistName = "Unknown Artist";
    }

    public Art(String id, String name, double basePrice) {
        this(id, name, "", basePrice, "", "Unknown Artist");
    }

    public Art(
            String id,
            String name,
            String description,
            double basePrice,
            String imageUrl,
            String artistName) {
        super(id, name, description, basePrice, imageUrl);
        this.artistName = artistName;
    }

    @Override
    public String getCategory() {
        return "ART";
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
        markAsUpdated();
    }
}
