package com.auction.server.model;

/** Represents an art item with artist information. */
public class Art extends Item {
  private static final long serialVersionUID = 1L;
  private String artistName;

  /** Initializes the shared fields of this art domain object. */
  public Art() {
    super();
    this.artistName = "Unknown Artist";
  }

  /** Initializes the shared fields of this art domain object. */
  public Art(String id, String name, double basePrice) {
    this(id, name, "", basePrice, "", "Unknown Artist");
  }

  /** Initializes the shared fields of this art domain object. */
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

  /** Updates artist name and records the entity modification time. */
  public void setArtistName(String artistName) {
    this.artistName = artistName;
    markAsUpdated();
  }
}
