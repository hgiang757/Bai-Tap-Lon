package com.auction.server.model;

import java.time.Instant;

public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;

    protected String password;
    protected String email;
    protected String role;

    public User() {
        super();
    }

    // Constructor 2 tham số (String id, String name)
    public User(String id, String name) {
        super(id, name);
    }

    // Constructor 2 tham số (Long id, String name)
    public User(Long id, String name) {
        super(id, name);
    }

    // Constructor 3 tham số (String id, String name, String password)
    public User(String id, String name, String password) {
        super(id, name);
        this.password = password;
    }

    // Constructor 3 tham số (Long id, String name, String password)
    public User(Long id, String name, String password) {
        super(id, name);
        this.password = password;
    }

    // Constructor 4 tham số (String id, String name, String password, String role)
    public User(String id, String name, String password, String role) {
        super(id, name);
        this.password = password;
        this.role = role;
    }

    // Constructor 4 tham số (Long id, String name, String password, String role)
    public User(Long id, String name, String password, String role) {
        super(id, name);
        this.password = password;
        this.role = role;
    }

    // Constructor 5 tham số
    public User(String id, String name, String password, String email, String role) {
        super(id, name);
        this.password = password;
        this.email = email;
        this.role = role;
    }

    public User(Long id, String name, String password, String email, String role) {
        super(id, name);
        this.password = password;
        this.email = email;
        this.role = role;
    }

    public String getUsername() {
        return getName();
    }

    public void setUsername(String username) {
        setName(username);
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

    // BẮT BUỘC CHO UserDAOTest.java dòng 33, 34, 35
    public String getRoleName() {
        return role;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", role='" + getRoleName() + '\'' +
                '}';
    }
}
