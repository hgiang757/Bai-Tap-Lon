package com.auction.server.model;

/** Các loại tài khoản dùng chung thông tin, khác nhau ở vai trò. */
public abstract class User extends Entity {
    private final String passwordHash;

    protected User(String id, String name, String passwordHash) {
        super(id, name);
        this.passwordHash = passwordHash;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public abstract String getRoleName();
}
