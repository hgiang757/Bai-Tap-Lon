package com.auction.server.model;

/**
 * Parameterized Factory Method: create() is overridden by the selected creator. The static
 * forCategory() selector is a Simple Factory for these creators.
 */
public abstract class ItemFactory {
  /** Creates a concrete item through the selected category creator. */
  public abstract Item create(String id, String name, String description, Seller owner);

  /** Selects a creator for a supported category or its recognized legacy alias. */
  public static ItemFactory forCategory(String category) {
    String input = category == null ? "" : category.trim().toUpperCase(java.util.Locale.ROOT);
    String type =
        switch (input) {
          case "TECHNOLOGY", "DIGITAL" -> "ELECTRONICS";
          case "VEHICLES", "AUTOMOTIVE", "CAR" -> "VEHICLE";
          case "FASHION", "WATCHES", "COLLECTIBLES" -> "ART";
          default -> input;
        };
    if (!java.util.Set.of("ELECTRONICS", "ART", "VEHICLE", "JEWELRY", "REAL_ESTATE", "OTHER")
        .contains(type)) {
      throw new IllegalArgumentException("Unknown category");
    }
    return new ItemFactory() {
      @Override
      public Item create(String id, String name, String description, Seller owner) {
        return switch (type) {
          case "ELECTRONICS" -> new Electronics().create(id, name, description, owner);
          case "ART" -> new Art().create(id, name, description, owner);
          case "VEHICLE" -> new Vehicle().create(id, name, description, owner);
          default -> new CategoryItem(id, name, description, owner, type);
        };
      }
    };
  }

  private static final class CategoryItem extends Item {
    private final String category;

    CategoryItem(String id, String name, String description, Seller owner, String category) {
      super(id, name, description, 0, "");
      setSeller(owner);
      this.category = category;
    }

    @Override
    public String getCategory() {
      return category;
    }
  }
}
