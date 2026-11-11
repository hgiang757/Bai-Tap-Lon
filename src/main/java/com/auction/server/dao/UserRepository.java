package com.auction.server.dao;

import com.auction.server.model.User;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {
    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Path storagePath;

    public UserRepository() {
        this(Paths.get("data", "users.dat"));
    }

    public UserRepository(Path storagePath) {
        this.storagePath = storagePath;
        loadFromDisk();
    }

    public synchronized User save(User user) {
        usersById.put(user.getId(), user);
        persistToDisk();
        return user;
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(usersById.get(id));
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return usersById.values().stream()
                .filter(u -> username.equalsIgnoreCase(u.getUsername()))
                .findFirst();
    }

    public List<User> findAll() {
        return new ArrayList<>(usersById.values());
    }

    public synchronized boolean deleteById(String id) {
        boolean removed = usersById.remove(id) != null;
        if (removed) persistToDisk();
        return removed;
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadFromDisk() {
        if (storagePath == null || !Files.exists(storagePath)) return;
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(storagePath))) {
            Map<String, User> loaded = (Map<String, User>) ois.readObject();
            usersById.clear();
            usersById.putAll(loaded);
        } catch (Exception ignored) {
            // Khởi tạo mới nếu file trống hoặc khác version
        }
    }

    private synchronized void persistToDisk() {
        if (storagePath == null) return;
        try {
            if (storagePath.getParent() != null) {
                Files.createDirectories(storagePath.getParent());
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(storagePath))) {
                oos.writeObject(new HashMap<>(usersById));
            }
        } catch (IOException e) {
            System.err.println("Không thể lưu file User: " + e.getMessage());
        }
    }
}
