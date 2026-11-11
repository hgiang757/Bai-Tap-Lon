// Hệ thống đấu giá có nhiều loại object, tất cả đều có thuộc tính chung nên viết 1 lớp abstract class

package com.auction.server.model;
import java.io.Serializable;

/** Thuộc tính chung của người dùng và sản phẩm. */
public abstract class Entity {
    private static final long serialVersionUID = 1L;
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
