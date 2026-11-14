package com.auction.server.model;

/** Represents the administrative role in the server user hierarchy. */
public class Admin extends User {
  private static final long serialVersionUID = 1L;

  /** Initializes the shared fields of this admin domain object. */
  public Admin() {
    super();
  }

  /** Initializes the shared fields of this admin domain object. */
  public Admin(String id, String name, String passwordHash) {
    super(id, name, passwordHash);
  }

  /** Initializes the shared fields of this admin domain object. */
  public Admin(String id, String name, String passwordHash, String email) {
    super(id, name, passwordHash, email, 0.0);
  }

  @Override
  public String getRoleName() {
    return "ADMIN";
  }
}
