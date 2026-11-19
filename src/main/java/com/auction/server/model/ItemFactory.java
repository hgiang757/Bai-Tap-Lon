package com.auction.server.model;

import java.util.Locale;

/** Selects an item creator by category and delegates construction to its factory method. */
public abstract class ItemFactory {
  /** Creates a concrete item through the selected category creator. */
  public abstract Item create(String id, String name, String description, Seller owner);

  /** Selects a creator for a supported category or its recognized legacy alias. */
  public static ItemFactory forCategory(String category) {
    String type = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
    return switch (type) {
      case "ELECTRONICS", "TECHNOLOGY", "DIGITAL" -> new ElectronicsFactory();
      case "ART", "FASHION", "WATCHES", "COLLECTIBLES" -> new ArtFactory();
      case "VEHICLE", "VEHICLES", "AUTOMOTIVE", "CAR" -> new VehicleFactory();
      case "JEWELRY", "REAL_ESTATE", "OTHER" -> new CategoryItemFactory(type);
      default -> throw new IllegalArgumentException("Unknown category");
    };
  }

  private static final class ElectronicsFactory extends ItemFactory {
    @Override
    public Item create(String id, String name, String description, Seller owner) {
      return new Electronics().create(id, name, description, owner);
    }
  }

  private static final class ArtFactory extends ItemFactory {
    @Override
    public Item create(String id, String name, String description, Seller owner) {
      return new Art().create(id, name, description, owner);
    }
  }

  private static final class VehicleFactory extends ItemFactory {
    @Override
    public Item create(String id, String name, String description, Seller owner) {
      return new Vehicle().create(id, name, description, owner);
    }
  }

  private static final class CategoryItemFactory extends ItemFactory {
    private final String category;

    CategoryItemFactory(String category) {
      this.category = category;
    }

    @Override
    public Item create(String id, String name, String description, Seller owner) {
      return new CategoryItem(id, name, description, owner, category);
    }
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
