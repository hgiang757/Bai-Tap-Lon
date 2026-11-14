package com.auction.server.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Provides identity, names and update timestamps for domain objects. */
public abstract class Entity implements Serializable {
  private static final long serialVersionUID = 1L;

  private String id;
  private String name;
  private Instant createdAt;
  private Instant updatedAt;

  /** Initializes the shared fields of this entity domain object. */
  public Entity() {
    this.id = UUID.randomUUID().toString();
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  /** Initializes the shared fields of this entity domain object. */
  public Entity(String id) {
    this(id, null);
  }

  /** Initializes the shared fields of this entity domain object. */
  public Entity(String id, String name) {
    this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
    this.name = name;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public String getId() {
    return id;
  }

  /** Updates id and records the entity modification time. */
  public void setId(String id) {
    this.id = id;
    markAsUpdated();
  }

  public String getName() {
    return name;
  }

  /** Updates name and records the entity modification time. */
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

  /** Records the current instant as the entity modification time. */
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
