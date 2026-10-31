package com.auction.server.model;

// Tính Kế thừa (Inheritance): User kế thừa Entity để lấy id và name (name ở đây dùng làm username/fullName)
public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;

    private String password;
    private double balance; // Số dư ví (Dành cho chức năng đặt cọc / thanh toán)

    public User(String id, String username, String password) {
        super(id, username); // Gọi constructor của lớp cha Entity
        this.password = password;
        this.balance = 0.0;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    // Phương thức trừu tượng thể hiện Đa hình (Polymorphism)
    public abstract String getRoleName();
}