package com.auction.server.model;

public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;

    private String passwordHash;
    private String email;
    private double balance;

    public User() {
        super();
        this.balance = 0.0;
    }

    public User(String id, String name) {
        super(id, name);
        this.balance = 0.0;
    }

    public User(String id, String name, String passwordHash) {
        super(id, name);
        this.passwordHash = passwordHash;
        this.balance = 0.0;
    }

    public User(String id, String name, String passwordHash, String email) {
        super(id, name);
        this.passwordHash = passwordHash;
        this.email = email;
        this.balance = 0.0;
    }

    public User(String id, String name, String passwordHash, String email, double balance) {
        super(id, name);
        this.passwordHash = passwordHash;
        this.email = email;
        this.balance = balance;
    }

    public User(String name, String passwordHash, String email) {
        super(null, name);
        this.passwordHash = passwordHash;
        this.email = email;
        this.balance = 0.0;
    }

    // Phương thức trừu tượng định danh vai trò (Admin, Seller, Bidder sẽ override)
    public abstract String getRoleName();

    // Các hàm phục vụ AuctionService
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        markAsUpdated();
    }

    // Các alias hỗ trợ cả 2 cách gọi name / username, password / passwordHash
    public String getUsername() {
        return getName();
    }

    public void setUsername(String username) {
        setName(username);
    }

    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String password) {
        setPasswordHash(password);
    }

    public String getRole() {
        return getRoleName();
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
            throw new IllegalStateException("Số dư tài khoản không đủ");
        }
        this.balance -= amount;
        markAsUpdated();
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", role='" + getRoleName() + '\'' +
                ", email='" + email + '\'' +
                ", balance=" + balance +
                '}';
    }
}
