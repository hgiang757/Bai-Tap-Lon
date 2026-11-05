package com.auction.server.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * Abstract base class cho tất cả entities trong hệ thống đấu giá.
 * Chứa các thuộc tính chung: id, createdAt, updatedAt
 */
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Instant createdAt;
    private Instant updatedAt;

    public Entity() {
        this.id = 1L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Entity(Long id) {
        this.id = id;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Entity(Long id, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
                "id=" + id +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
