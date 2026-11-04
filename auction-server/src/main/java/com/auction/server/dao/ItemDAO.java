package com.auction.server.dao;

import com.auction.server.model.Item;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Lưu/tải Item (Electronics, Art, ...) ra file. */
public class ItemDAO extends FileDao<Item> {

    public ItemDAO(Path file) {
        super(file);
    }

    public Optional<Item> findById(String id) throws IOException {
        if (id == null) {
            return Optional.empty();
        }
        return loadAll().stream()
                .filter(i -> id.equals(i.getId()))
                .findFirst();
    }
}
