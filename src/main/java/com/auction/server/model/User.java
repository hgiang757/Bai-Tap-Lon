package com.auction.server.model;

//Tạo abstract class user
public abstract class User extends Entity {
  private static final long serialVersionUID = 1L;

  private String passwordHash;
  private String email;
  private double balance;
  private boolean active;

  //Constructor
  public User() {
    super();
    this.active = true;
  }


  public User(String id, String name, String passwordHash) {
    this(id, name, passwordHash, "", 0.0);
  }


  public User(String id, String name, String passwordHash, String email, double balance) {
    super(id, name);
    this.passwordHash = passwordHash;
    this.email = email;
    this.balance = balance;
    this.active = true;
  }


  public abstract String getRoleName();

  public UserRole getRole() {
    return UserRole.valueOf(getRoleName());
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  //Cập nhật password và thời gian
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
    markAsUpdated();
  }

  public String getUsername() {
    return getName();
  }

  //Cập nhật username và thời gian
  public void setUsername(String username) {
    setName(username);
  }

  public String getEmail() {
    return email;
  }

  //Cập nhật email và thời gian
  public void setEmail(String email) {
    this.email = email;
    markAsUpdated();
  }

  public double getBalance() {
    return balance;
  }

  //Cập nhật số dư và thời gian
  public void setBalance(double balance) {
    this.balance = balance;
    markAsUpdated();
  }

  public boolean isActive() {
    return active;
  }


  public void setActive(boolean active) {
    this.active = active;
    markAsUpdated();
  }


  public void deposit(double amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
    }
    this.balance += amount;
    markAsUpdated();
  }


  public void deduct(double amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Số tiền trừ phải lớn hơn 0");
    }
    if (this.balance < amount) {
      throw new IllegalStateException("Số dư không đủ");
    }
    this.balance -= amount;
    markAsUpdated();
  }
}
