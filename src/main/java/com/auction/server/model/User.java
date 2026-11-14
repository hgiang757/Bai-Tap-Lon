package com.auction.server.model;

/** Defines shared account data and the role contract for concrete user types. */
public abstract class User extends Entity {
  private static final long serialVersionUID = 1L;

  private String passwordHash;
  private String email;
  private double balance;
  private boolean active;

  /** Initializes the shared fields of this user domain object. */
  public User() {
    super();
    this.active = true;
  }

  /** Initializes the shared fields of this user domain object. */
  public User(String id, String name, String passwordHash) {
    this(id, name, passwordHash, "", 0.0);
  }

  /** Initializes the shared fields of this user domain object. */
  public User(String id, String name, String passwordHash, String email, double balance) {
    super(id, name);
    this.passwordHash = passwordHash;
    this.email = email;
    this.balance = balance;
    this.active = true;
  }

  /** Returns the role defined by the concrete user type. */
  public abstract String getRoleName();

  public UserRole getRole() {
    return UserRole.valueOf(getRoleName());
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  /** Updates password hash and records the entity modification time. */
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
    markAsUpdated();
  }

  public String getUsername() {
    return getName();
  }

  /** Updates username and records the entity modification time. */
  public void setUsername(String username) {
    setName(username);
  }

  public String getEmail() {
    return email;
  }

  /** Updates email and records the entity modification time. */
  public void setEmail(String email) {
    this.email = email;
    markAsUpdated();
  }

  public double getBalance() {
    return balance;
  }

  /** Updates balance and records the entity modification time. */
  public void setBalance(double balance) {
    this.balance = balance;
    markAsUpdated();
  }

  public boolean isActive() {
    return active;
  }

  /** Updates active and records the entity modification time. */
  public void setActive(boolean active) {
    this.active = active;
    markAsUpdated();
  }

  /** Adds a positive amount to this legacy model balance. */
  public void deposit(double amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
    }
    this.balance += amount;
    markAsUpdated();
  }

  /** Deducts a positive amount only when this legacy model balance is sufficient. */
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
