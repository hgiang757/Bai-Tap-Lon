package com.auction.server.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ItemFactoryTest {
    @ParameterizedTest
    @ValueSource(strings = {"ELECTRONICS", "ART", "VEHICLE", "JEWELRY", "REAL_ESTATE", "OTHER"})
    void createsCorrectCategoryAndPreservesProductAndOwner(String category) {
        Seller owner = new Seller("seller-id", "seller", "hash");
        Item item = ItemFactory.forCategory(category).create("item-id", "Sản phẩm", "Mô tả", owner);
        assertEquals(category, item.getCategory());
        assertEquals("item-id", item.getId());
        assertEquals("Sản phẩm", item.getName());
        assertEquals("Mô tả", item.getDescription());
        assertSame(owner, item.getSeller());
        switch (category) {
            case "ELECTRONICS" -> assertInstanceOf(Electronics.class, item);
            case "ART" -> assertInstanceOf(Art.class, item);
            case "VEHICLE" -> assertInstanceOf(Vehicle.class, item);
            default -> assertInstanceOf(Item.class, item);
        }
    }

    @Test
    void rejectsUnknownCategoryInsteadOfSilentlyCreatingOther() {
        assertThrows(IllegalArgumentException.class, () -> ItemFactory.forCategory("UNKNOWN"));
    }

    @Test
    void userRoleDispatchUsesTheRuntimeSubclass() {
        List<User> users = List.of(new Bidder("b", "buyer", "hash"),
                new Seller("s", "seller", "hash"), new Admin("a", "admin", "hash"));
        assertEquals(List.of("BIDDER", "SELLER", "ADMIN"), users.stream().map(User::getRoleName).toList());
    }
}
