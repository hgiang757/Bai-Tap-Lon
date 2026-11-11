package com.auction.server.model;

import java.util.Objects;

public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String email;
    private double balance;

    public User() {
        super();
    }

    public User(String username, String password, String email) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.balance = 0.0;
    }

    public User(Long id, String username, String password, String email, double balance) {
        super(id);
        this.username = username;
        this.password = password;
        this.email = email;
        this.balance = balance;
    }

    // Đa hình (Polymorphism): Các lớp con bắt buộc phải triển khai vai trò của mình
    public abstract UserRole getRole();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        markAsUpdated();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        markAsUpdated();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        markAsUpdated();
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
        markAsUpdated();
    }

    public void addBalance(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        this.balance += amount;
        markAsUpdated();
    }

    public void deductBalance(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền trừ phải lớn hơn 0");
        }
        if (this.balance < amount) {
            throw new IllegalStateException("Số dư tài khoản không đủ để thực hiện giao dịch");
        }
        this.balance -= amount;
        markAsUpdated();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role=" + getRole() +
                ", balance=" + balance +
                '}';
    }
}
