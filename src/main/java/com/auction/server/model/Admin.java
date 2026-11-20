package com.auction.server.model;

/** Đại diện cho tài khoản quản trị trên máy chủ. */
public class Admin extends User {
  private static final long serialVersionUID = 1L;

  /** Khởi tạo thông tin cơ sở của tài khoản quản trị. */
  public Admin() {
    super();
  }

  /** Khởi tạo thông tin cơ sở của tài khoản quản trị. */
  public Admin(String id, String name, String passwordHash) {
    super(id, name, passwordHash);
  }

  /** Khởi tạo thông tin cơ sở của tài khoản quản trị. */
  public Admin(String id, String name, String passwordHash, String email) {
    super(id, name, passwordHash, email, 0.0);
  }

  @Override
  public String getRoleName() {
    return "ADMIN";
  }
}
