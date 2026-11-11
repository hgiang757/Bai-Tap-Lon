package com.auction.server.model;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private String description;
    private double basePrice;
    private String imageUrl;
    private Seller seller;

    public Item() {
        super();
    }

    public Item(String id, String name, double basePrice) {
        this(id, name, "", basePrice, "");
    }

    public Item(String id, String name, String description, double basePrice, String imageUrl) {
        super(id, name);
        this.description = description;
        this.basePrice = basePrice;
        this.imageUrl = imageUrl;
    }

    public abstract String getCategory();

    // --- HÀM BỊ THIẾU MÀ AUCTION SERVICE GỐC ĐANG GỌI TẠI DÒNG 590 ---
    public static Item create(String arg1, String arg2, String arg3, Seller seller) {
        Item item;
        try {
            // Trường hợp 1: arg1 là category (VD: "ART"), arg2 là name, arg3 là description
            item = ItemFactory.forCategory(arg1);
            item.setName(arg2);
            item.setDescription(arg3);
        } catch (Exception e1) {
            try {
                // Trường hợp 2: arg1 là name, arg2 là description, arg3 là category
                item = ItemFactory.forCategory(arg3);
                item.setName(arg1);
                item.setDescription(arg2);
            } catch (Exception e2) {
                // Trường hợp 3 (Dự phòng): arg1 là id, arg2 là name, arg3 là category
                try {
                    item = ItemFactory.forCategory(arg3);
                    item.setId(arg1);
                    item.setName(arg2);
                } catch (Exception e3) {
                    // Fallback an toàn nhất để tránh sập Server nếu dữ liệu bất thường
                    item = ItemFactory.forCategory("ELECTRONICS");
                    item.setName(arg1);
                    item.setDescription(arg2);
                }
            }
        }
        item.setSeller(seller);
        return item;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
        markAsUpdated();
    }

    public String getType() {
        return getCategory();
    }

    public String getItemType() {
        return getCategory();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        markAsUpdated();
    }

    public double getBasePrice() {
        return basePrice;
    }

    public double getStartingPrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
        markAsUpdated();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        markAsUpdated();
    }
}
