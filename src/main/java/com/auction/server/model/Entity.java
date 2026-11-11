package com.auction.server.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;

    public Entity() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Entity(String id) {
        this();
        this.id = id;
    }

    // Constructor bắt buộc để khớp với Item.java dòng 9 và User.java
    public Entity(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        markAsUpdated();
    }

    // Hàm bắt buộc để khớp với AuctionService.java dòng 187
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        markAsUpdated();
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

    public void markAsUpdated() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
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
