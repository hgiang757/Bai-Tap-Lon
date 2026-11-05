package com.auction.server.model;

import java.time.Instant;

public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;

    private String name;
    private String password;
    private String email;
    private String role;

    public User() {
        super();
    }

    public User(Long id, String name, String password, String role) {
        super(id);
        this.name = name;
        this.password = password;
        this.role = role;
    }

    public User(Long id, String name, String password, String email, String role) {
        super(id);
        this.name = name;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    // UserDAO.java gọi u.getName() nên bắt buộc phải có hàm này
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setUpdatedAt(Instant.now());
    }

    public String getUsername() {
        return name;
    }

    public void setUsername(String username) {
        this.name = username;
        setUpdatedAt(Instant.now());
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        setUpdatedAt(Instant.now());
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        setUpdatedAt(Instant.now());
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
        setUpdatedAt(Instant.now());
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
