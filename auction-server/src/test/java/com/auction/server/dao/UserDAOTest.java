package com.auction.server.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.server.model.Admin;
import com.auction.server.model.Bidder;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserDAOTest {

    @TempDir
    Path tempDir;

    @Test
    void saveThenLoad_keepsUsersAndRoles() throws IOException {
        UserDAO dao = new UserDAO(tempDir.resolve("users.dat"));
        dao.saveAll(List.of(
                new Bidder("1", "alice", "pw1"),
                new Seller("2", "bob", "pw2"),
                new Admin("3", "root", "pw3")));

        List<User> loaded = dao.loadAll();

        assertEquals(3, loaded.size());
        assertEquals("BIDDER", loaded.get(0).getRoleName());
        assertEquals("SELLER", loaded.get(1).getRoleName());
        assertEquals("ADMIN", loaded.get(2).getRoleName());
    }

    @Test
    void findByUsername_ignoresCase() throws IOException {
        UserDAO dao = new UserDAO(tempDir.resolve("users.dat"));
        dao.saveAll(List.of(new Bidder("1", "Alice", "pw")));

        Optional<User> found = dao.findByUsername("alice");

        assertTrue(found.isPresent());
        assertEquals("1", found.get().getId());
    }

    @Test
    void findByUsername_unknownOrNull_returnsEmpty() throws IOException {
        UserDAO dao = new UserDAO(tempDir.resolve("users.dat"));
        dao.saveAll(List.of(new Bidder("1", "alice", "pw")));

        assertTrue(dao.findByUsername("ghost").isEmpty());
        assertTrue(dao.findByUsername(null).isEmpty());
    }
}
