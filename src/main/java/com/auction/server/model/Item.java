package com.auction.server.model;

/** Lớp cha của các danh mục sản phẩm; Factory tạo lớp con tương ứng. */
public abstract class Item extends Entity {
    private final String description;
    private final Seller seller;

    protected Item(String id, String name, String description, Seller seller) {
        super(id, name);
        this.description = description;
        this.seller = seller;
    }

    public String getDescription() {
        return description;
    }

    public Seller getSeller() {
        return seller;
    }

    public abstract String getCategory();
}
