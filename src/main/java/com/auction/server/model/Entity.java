// Hệ thống đấu giá có nhiều loại object, tất cả đều có thuộc tính chung nên viết 1 lớp abstract class

package com.auction.server.model;

import java.io.Serializable;
import java.time.Instant;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Instant createdAt;
    private Instant updatedAt;

    public Entity() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Entity(Long id) {
        this();
        this.id = id;
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

    public void markAsUpdated() {
        this.updatedAt = Instant.now();
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
