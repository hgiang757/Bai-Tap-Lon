package com.auction.server.model;

public class ItemFactory {

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
            case "ELECTRONICS" -> {
                int warranty = parseIntOrDefault(extraInfo, 12);
                yield new Electronics(id, name, description, basePrice, imageUrl, warranty);
            }
            case "ART" -> {
                String artist = (extraInfo == null || extraInfo.isBlank()) ? "Unknown" : extraInfo;
                yield new Art(id, name, description, basePrice, imageUrl, artist);
            }
            case "VEHICLE" -> {
                int mileage = parseIntOrDefault(extraInfo, 0);
                yield new Vehicle(id, name, description, basePrice, imageUrl, mileage);
            }
            default -> throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
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
