package com.auction.server.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Lớp cơ sở chứa mã định danh, tên và thời điểm tạo, cập nhật của thực thể. */
public abstract class Entity implements Serializable {
  private static final long serialVersionUID = 1L;

  private String id;
  private String name;
  private Instant createdAt;
  private Instant updatedAt;

  /** Tạo thực thể với mã định danh tự sinh. */
  public Entity() {
    this.id = UUID.randomUUID().toString();
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  /** Tạo thực thể với mã định danh đã có. */
  public Entity(String id) {
    this(id, null);
  }

  /** Tạo thực thể với tên và mã định danh, tự sinh mã nếu để trống. */
  public Entity(String id, String name) {
    this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
    this.name = name;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public String getId() {
    return id;
  }

  /** Đổi mã định danh và ghi nhận thời điểm cập nhật. */
  public void setId(String id) {
    this.id = id;
    markAsUpdated();
  }

  public String getName() {
    return name;
  }

  /** Đổi tên và ghi nhận thời điểm cập nhật. */
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

  /** Ghi nhận thời điểm cập nhật là thời gian hiện tại. */
  public void markAsUpdated() {
    this.updatedAt = Instant.now();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Entity entity = (Entity) o;
    return Objects.equals(id, entity.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
