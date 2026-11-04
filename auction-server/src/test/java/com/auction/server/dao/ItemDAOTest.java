package com.auction.server.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.server.model.Item;
import com.auction.server.model.Seller;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ItemDAOTest {

    /** Item tối thiểu (static để serialize được, không kéo theo lớp test). */
    private static class TestItem extends Item {
        private static final long serialVersionUID = 1L;

        TestItem(String id, String name) {
            super(id, name, "mô tả", (Seller) null);
        }

        @Override
        public double calculateInsuranceFee(double currentPrice) {
            return currentPrice * 0.01;
        }
    }

    @TempDir
    Path tempDir;

    @Test
    void saveThenLoad_keepsItems() throws IOException {
        ItemDAO dao = new ItemDAO(tempDir.resolve("items.dat"));
        dao.saveAll(List.of(new TestItem("i1", "Đồng hồ"), new TestItem("i2", "Tranh")));

        List<Item> loaded = dao.loadAll();

        assertEquals(2, loaded.size());
        assertEquals("Đồng hồ", loaded.get(0).getName());
    }

    @Test
    void findById_returnsMatchingItem() throws IOException {
        ItemDAO dao = new ItemDAO(tempDir.resolve("items.dat"));
        dao.saveAll(List.of(new TestItem("i1", "Đồng hồ"), new TestItem("i2", "Tranh")));

        Optional<Item> found = dao.findById("i2");

        assertTrue(found.isPresent());
        assertEquals("Tranh", found.get().getName());
        assertTrue(dao.findById("nope").isEmpty());
        assertTrue(dao.findById(null).isEmpty());
    }
}
