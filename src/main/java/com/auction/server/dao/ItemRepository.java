package com.auction.server.dao;

import com.auction.server.model.Item;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemRepository {
    private final Map<String, Item> itemsById = new ConcurrentHashMap<>();
    private final Path storagePath;

    public ItemRepository() {
        this(Paths.get("data", "items.dat"));
    }

    public ItemRepository(Path storagePath) {
        this.storagePath = storagePath;
        loadFromDisk();
    }

    public synchronized Item save(Item item) {
        itemsById.put(item.getId(), item);
        persistToDisk();
        return item;
    }

    public Optional<Item> findById(String id) {
        return Optional.ofNullable(itemsById.get(id));
    }

    public List<Item> findAll() {
        return new ArrayList<>(itemsById.values());
    }

    public synchronized boolean deleteById(String id) {
        boolean removed = itemsById.remove(id) != null;
        if (removed) persistToDisk();
        return removed;
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadFromDisk() {
        if (storagePath == null || !Files.exists(storagePath)) return;
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(storagePath))) {
            Map<String, Item> loaded = (Map<String, Item>) ois.readObject();
            itemsById.clear();
            itemsById.putAll(loaded);
        } catch (Exception ignored) {
        }
    }

    private synchronized void persistToDisk() {
        if (storagePath == null) return;
        try {
            if (storagePath.getParent() != null) {
                Files.createDirectories(storagePath.getParent());
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(storagePath))) {
                oos.writeObject(new HashMap<>(itemsById));
            }
        } catch (IOException e) {
            System.err.println("Không thể lưu file Item: " + e.getMessage());
        }
    }
}
