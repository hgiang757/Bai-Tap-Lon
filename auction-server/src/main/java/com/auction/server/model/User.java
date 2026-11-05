package com.auction.server.model;

import java.time.Instant;

public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;

    protected String password;

    public User() {
        super();
    }

    public User(String id, String name, String password) {
        super(id, name);
        this.password = password;
    }

    public User(Long id, String name, String password) {
        super(id, name);
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        setUpdatedAt(Instant.now());
    }

    // Bắt buộc cho UserDAOTest: assertEquals("BIDDER", loaded.get(0).getRoleName());
    public abstract String getRoleName();
}
