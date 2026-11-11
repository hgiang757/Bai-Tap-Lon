package com.auction.server.model;

public class ItemFactory {

    public static Item forCategory(String category) {
        if (category == null || category.isBlank()) {
            return new Art();
        }
        return switch (category.trim().toUpperCase()) {
            case "ELECTRONICS", "TECHNOLOGY", "DIGITAL" -> new Electronics();
            case "VEHICLE", "VEHICLES", "AUTOMOTIVE", "CAR" -> new Vehicle();
            case "ART", "JEWELRY", "REAL_ESTATE", "FASHION", "WATCHES", "COLLECTIBLES", "OTHER" -> new Art();
            default -> new Art();
        };
    }

    public static Item create(String type, String id, String name, double basePrice) {
        return createItem(type, id, name, basePrice);
    }

    public static Item create(String type, String name, double basePrice) {
        return createItem(type, null, name, basePrice);
    }

    public static Item createItem(String type, String name, double basePrice) {
        return createItem(type, null, name, basePrice);
    }

    public static Item createItem(String type, String id, String name, double basePrice) {
        return createItem(type, id, name, "", basePrice, "", null);
    }

    public static Item createItem(
            String type,
            String id,
            String name,
            String description,
            double basePrice,
            String imageUrl,
            String extraInfo) {

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Loại sản phẩm không được để trống");
        }
        if (basePrice < 0) {
            throw new IllegalArgumentException("Giá khởi điểm không được âm");
        }

        return switch (type.trim().toUpperCase()) {
            case "ELECTRONICS", "TECHNOLOGY", "DIGITAL" -> {
                int warranty = parseIntOrDefault(extraInfo, 12);
                yield new Electronics(id, name, description, basePrice, imageUrl, warranty);
            }
            case "VEHICLE", "VEHICLES", "AUTOMOTIVE", "CAR" -> {
                int mileage = parseIntOrDefault(extraInfo, 0);
                yield new Vehicle(id, name, description, basePrice, imageUrl, mileage);
            }
            default -> {
                String artist = (extraInfo == null || extraInfo.isBlank()) ? "Unknown Artist" : extraInfo;
                yield new Art(id, name, description, basePrice, imageUrl, artist);
            }
        };
    }

    private static int parseIntOrDefault(String value, int defaultVal) {
        if (value == null || value.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
