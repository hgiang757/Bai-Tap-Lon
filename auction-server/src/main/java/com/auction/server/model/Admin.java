package com.auction.server.model;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin() {
        super();
        setRole("ADMIN");
    }

    public Admin(String id, String name) {
        super(id, name, "", "ADMIN");
    }

    public Admin(Long id, String name) {
        super(id, name, "", "ADMIN");
    }

    public Admin(String id, String name, String password) {
        super(id, name, password, "ADMIN");
    }

    public Admin(Long id, String name, String password) {
        super(id, name, password, "ADMIN");
    }

    public Admin(String id, String name, String password, String email) {
        super(id, name, password, email, "ADMIN");
    }

    public Admin(Long id, String name, String password, String email) {
        super(id, name, password, email, "ADMIN");
    }

    @Override
    public String getRoleName() {
        return "ADMIN";
    }
}
