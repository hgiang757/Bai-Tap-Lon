package com.auction.server.model;

/** Thuộc tính chung của người dùng và sản phẩm. */
public abstract class Entity {
    private final String id;
    private final String name;

    protected Entity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
