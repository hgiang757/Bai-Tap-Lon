package com.auction.server.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileDaoTest {

    @TempDir
    Path tempDir;

    @Test
    void loadFromMissingFile_returnsEmptyList() throws IOException {
        FileDao<String> dao = new FileDao<>(tempDir.resolve("none.dat"));
        assertTrue(dao.loadAll().isEmpty());
    }

    @Test
    void saveThenLoad_returnsSameData() throws IOException {
        FileDao<String> dao = new FileDao<>(tempDir.resolve("data/items.dat"));
        dao.saveAll(List.of("a", "b", "c"));
        assertEquals(List.of("a", "b", "c"), dao.loadAll());
    }

    @Test
    void saveTwice_overwritesOldData() throws IOException {
        FileDao<String> dao = new FileDao<>(tempDir.resolve("items.dat"));
        dao.saveAll(List.of("a", "b"));
        dao.saveAll(List.of("z"));
        assertEquals(List.of("z"), dao.loadAll());
    }
}
