package com.auction.server.dao;

import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Transaction;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionRepository {
    private final Map<String, Auction> auctionsById = new ConcurrentHashMap<>();
    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();
    private final Path storagePath;

    public AuctionRepository() {
        this(Paths.get("data", "auctions.dat"));
    }

    public AuctionRepository(Path storagePath) {
        this.storagePath = storagePath;
        loadFromDisk();
    }

    public synchronized Auction save(Auction auction) {
        auctionsById.put(auction.getId(), auction);
        persistToDisk();
        return auction;
    }

    public Optional<Auction> findById(String id) {
        return Optional.ofNullable(auctionsById.get(id));
    }

    public List<Auction> findAll() {
        return new ArrayList<>(auctionsById.values());
    }

    public List<Auction> findByStatus(AuctionStatus status) {
        return auctionsById.values().stream()
                .filter(a -> a.getStatus() == status)
                .toList();
    }

    public synchronized boolean deleteById(String id) {
        boolean removed = auctionsById.remove(id) != null;
        if (removed) persistToDisk();
        return removed;
    }

    public synchronized void recordTransaction(Transaction tx) {
        transactions.add(tx);
        persistToDisk();
    }

    public List<Transaction> findTransactionsByUser(String userId) {
        return transactions.stream()
                .filter(t -> Objects.equals(userId, t.getUserId()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadFromDisk() {
        if (storagePath == null || !Files.exists(storagePath)) return;
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(storagePath))) {
            Map<String, Auction> loaded = (Map<String, Auction>) ois.readObject();
            auctionsById.clear();
            auctionsById.putAll(loaded);
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
                oos.writeObject(new HashMap<>(auctionsById));
            }
        } catch (IOException e) {
            System.err.println("Không thể lưu file Auction: " + e.getMessage());
        }
    }
}
