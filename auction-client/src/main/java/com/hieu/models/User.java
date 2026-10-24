package com.hieu.models;

import java.io.Serializable;

/**
 * Entity đại diện cho người dùng (Bidder hoặc Seller).
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String fullName;
    private String email;
    private UserRole role;
    private double balance;

    public User() {
    }

    public User(String id, String username, String fullName, String email, UserRole role, double balance) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.balance = balance;
    }

    // Getters and Setters (Encapsulation)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", balance=" + balance +
                '}';
    }
}
