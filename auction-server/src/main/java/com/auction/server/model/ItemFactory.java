package com.auction.server.model;

/** Factory Method for the six client product categories. */
public abstract class ItemFactory {
    public abstract Item create(String id, String name, String description, Seller owner);
    public static ItemFactory forCategory(String category) {
        if (!java.util.Set.of("ELECTRONICS", "ART", "VEHICLE", "JEWELRY", "REAL_ESTATE", "OTHER").contains(category))
            throw new IllegalArgumentException("Unknown category");
        return new ItemFactory() {
            @Override public Item create(String id, String name, String description, Seller owner) {
                return switch (category) {
                    case "ELECTRONICS" -> new ElectronicsItem(id, name, description, owner, 0);
                    case "ART" -> new ArtItem(id, name, description, owner, "");
                    default -> new CategoryItem(id, name, description, owner, category);
                };
            }
        };
    }
    private static final class CategoryItem extends Item {
        private static final long serialVersionUID = 1L;
        private final String category;
        CategoryItem(String id, String name, String description, Seller owner, String category) {
            super(id, name, description, owner); this.category = category;
        }
        @Override public double calculateInsuranceFee(double price) { return price * ("VEHICLE".equals(category) ? 0.02 : 0); }
    }
}
