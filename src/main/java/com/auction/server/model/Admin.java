package com.auction.server.model;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin() {
        super();
    }

    public Admin(String username, String password, String email) {
        super(username, password, email);
    }

    public Admin(Long id, String username, String password, String email) {
        super(id, username, password, email, 0.0);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                '}';
    }
}
