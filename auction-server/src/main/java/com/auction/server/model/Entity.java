package com.auction.server.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * Abstract base class cho tất cả entities trong hệ thống đấu giá.
 * Chứa các thuộc tính chung: id, name, createdAt, updatedAt
 */
// ABSTRACTION
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    // ENCAPSULATION: ẩn thuộc tính, truy cập qua getter/setter
    protected String id;
    protected String name;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructor mặc định
    public Entity() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Constructor 1 tham số id
    public Entity(String id) {
        this.id = id;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Constructor 2 tham số (id, name) - BẮT BUỘC ĐỂ KHỚP VỚI User.java VÀ Item.java
    public Entity(String id, String name) {
        this.id = id;
        this.name = name;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Entity(String id, String name, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // GETTER & SETTER
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.updatedAt = Instant.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
