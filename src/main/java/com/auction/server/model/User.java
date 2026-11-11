package com.auction.server.model;

// INHERITANCE: User kế thừa Entity
public abstract class User extends Entity {
    private String username;
    private String password; // Trong thực tế sẽ được hash
    private String email;
    private double balance;  // Số dư ví

    public User(String username, String password, String email) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.balance = 0.0;
    }

    // Đa hình (Polymorphism): Các lớp con bắt buộc phải khai báo Role của mình
    public abstract UserRole getRole();

    public void addBalance(double amount) {
        this.balance += amount;
        markAsUpdated(); // Cập nhật thời gian sửa
    }
    
    public void deductBalance(double amount) /* throws InsufficientBalanceException */ {
        if (this.balance < amount) {
            throw new IllegalArgumentException("Không đủ số dư");
        }
        this.balance -= amount;
        markAsUpdated();
    }
    // Getters & Setters...
}
