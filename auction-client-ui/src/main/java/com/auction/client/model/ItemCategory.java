package com.auction.client.model;

import java.util.Locale;

/** Danh mục sản phẩm và biểu tượng dùng chung trong giao diện. */
public enum ItemCategory {
    ELECTRONICS("Điện tử", "laptop"), ART("Nghệ thuật", "image"), VEHICLE("Xe cộ", "car"),
    JEWELRY("Đồng hồ và trang sức", "watch"), REAL_ESTATE("Bất động sản", "home"), OTHER("Khác", "tag");
    private final String label;
    private final String icon;
    ItemCategory(String label, String icon) { this.label = label; this.icon = icon; }
    /** Tên tiếng Việt. */
    public String getLabel() { return label; }
    /** Tên biểu tượng trong Icons. */
    public String getIcon() { return icon; }
    /** Nhận diện cả dữ liệu mẫu cũ và tên danh mục hiện tại. */
    public static ItemCategory from(String value) {
        if (value == null) return OTHER;
        String s = value.toLowerCase(Locale.ROOT);
        for (ItemCategory c : values()) if (c.name().equalsIgnoreCase(value) || c.label.equalsIgnoreCase(value)) return c;
        if (s.contains("electronic") || s.contains("công nghệ") || s.contains("thiết bị") || s.contains("điện tử")) return ELECTRONICS;
        if (s.contains("art") || s.contains("nghệ thuật") || s.contains("cổ vật") || s.contains("tranh")) return ART;
        if (s.contains("vehicle") || s.contains("xe cộ")) return VEHICLE;
        if (s.contains("jewelry") || s.contains("đồng hồ") || s.contains("trang sức")) return JEWELRY;
        if (s.contains("real_estate") || s.contains("bất động sản")) return REAL_ESTATE;
        return OTHER;
    }
    @Override public String toString() { return label; }
}
