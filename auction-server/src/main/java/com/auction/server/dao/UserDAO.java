package com.auction.server.dao;

import com.auction.server.model.User;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Lưu/tải User (Bidder, Seller, Admin) ra file. Tên đăng nhập là {@code User.getName()}. */
public class UserDAO extends FileDao<User> {

    public UserDAO(Path file) {
        super(file);
    }

    public Optional<User> findByUsername(String username) throws IOException {
        if (username == null) {
            return Optional.empty();
        }
        return loadAll().stream()
                .filter(u -> username.equalsIgnoreCase(u.getName()))
                .findFirst();
    }
}
