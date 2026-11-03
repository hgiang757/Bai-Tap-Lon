package com.auction.client.service;

import com.auction.client.model.UserRole;
import com.auction.client.service.mock.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MockAuthIntegrationTest {
    @Test void registeredAccountSharesManagementState() {
        var store = MockDataStore.createForTesting();
        var auth = new MockAuctionClientService(store);
        var management = new MockManagementService(store);
        assertTrue(auth.register("new_user", "Password123", "New User", "new@example.com", UserRole.BIDDER));
        String id = auth.getCurrentUser().getId();
        assertTrue(management.listUsers().stream().anyMatch(u -> u.getUser().getId().equals(id)));
        auth.logout();
        management.setLocked("U-admin_hieu", id, true);
        assertFalse(auth.login("new_user", "Password123", null));
        management.setLocked("U-admin_hieu", id, false);
        management.setRole("U-admin_hieu", id, UserRole.SELLER);
        assertTrue(auth.login("new_user", "Password123", null));
        assertEquals(UserRole.SELLER, auth.getCurrentUser().getRole());
        auth.logout();
        management.deleteUser("U-admin_hieu", id);
        assertFalse(auth.login("new_user", "Password123", null));
    }

    @Test void sampleLoginUsesTheSameStoreAsManagement() {
        var store = MockDataStore.createForTesting();
        var auth = new MockAuctionClientService(store);
        var management = new MockManagementService(store);
        management.setLocked("U-admin_hieu", "U-bidder_hieu", true);
        assertFalse(auth.login("bidder_hieu", "123456", null));
    }
}
