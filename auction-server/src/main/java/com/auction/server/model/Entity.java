package com.auction.server.model;

import java.io.Serializable;
import java.time.LocalDateTime;

// Áp dụng tính Trừu tượng (Abstraction): Lớp nền tảng không thể khởi tạo trực tiếp
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    // Đóng gói (Encapsulation): Thuộc tính private, truy cập qua getter/setter
    private String id;
    private String name;
    private LocalDateTime createdAt;

    public Entity(String id, String name) {
        this.id = id;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
