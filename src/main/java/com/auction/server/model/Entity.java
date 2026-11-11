// Hệ thống đấu giá có nhiều loại object, tất cả đều có thuộc tính chung nên viết 1 lớp abstract class

package com.auction.server.model;
import java.io.Serializable;

/** Thuộc tính chung của người dùng và sản phẩm. */


// ABSTRACTION: Lớp trừu tượng định nghĩa các thuộc tính cơ bản
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // ENCAPSULATION: Các thuộc tính được đóng gói (private)
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

    // Hàm gọi khi có dữ liệu bên trong thay đổi
    public void markAsUpdated() {
        this.updatedAt = Instant.now();
    }

    // Getters và Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
