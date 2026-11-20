package com.auction.server.model;

/** Lớp cơ sở của tài khoản đặt giá, bán hàng và quản trị. */
public abstract class User extends Entity {
  private static final long serialVersionUID = 1L;

  private String passwordHash;
  private String email;
  private double balance;
  private boolean active;

  /** Tạo tài khoản hoạt động với mã định danh tự sinh. */
  public User() {
    super();
    this.active = true;
  }

  /** Tạo tài khoản với số dư bằng 0 và địa chỉ email trống. */
  public User(String id, String name, String passwordHash) {
    this(id, name, passwordHash, "", 0.0);
  }

  /** Tạo tài khoản hoạt động từ thông tin định danh và số dư đã có. */
  public User(String id, String name, String passwordHash, String email, double balance) {
    super(id, name);
    this.passwordHash = passwordHash;
    this.email = email;
    this.balance = balance;
    this.active = true;
  }

  /** Trả về mã vai trò do lớp tài khoản cụ thể xác định. */
  public abstract String getRoleName();

  public UserRole getRole() {
    return UserRole.valueOf(getRoleName());
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  /** Đổi giá trị băm mật khẩu và ghi nhận thời điểm cập nhật. */
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
    markAsUpdated();
  }

  public String getUsername() {
    return getName();
  }

  /** Đổi tên đăng nhập và ghi nhận thời điểm cập nhật. */
  public void setUsername(String username) {
    setName(username);
  }

  public String getEmail() {
    return email;
  }

  /** Đổi email và ghi nhận thời điểm cập nhật. */
  public void setEmail(String email) {
    this.email = email;
    markAsUpdated();
  }

  public double getBalance() {
    return balance;
  }

  /** Đổi số dư và ghi nhận thời điểm cập nhật. */
  public void setBalance(double balance) {
    this.balance = balance;
    markAsUpdated();
  }

  public boolean isActive() {
    return active;
  }

  /** Đổi trạng thái hoạt động của tài khoản và ghi nhận thời điểm cập nhật. */
  public void setActive(boolean active) {
    this.active = active;
    markAsUpdated();
  }

  /** Cộng một khoản tiền dương vào số dư của đối tượng. */
  public void deposit(double amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
    }
    this.balance += amount;
    markAsUpdated();
  }

  /** Trừ một khoản tiền dương khi số dư của đối tượng đủ. */
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
