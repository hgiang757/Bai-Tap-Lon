package com.auction.client.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.auction.client.model.AuctionStatus;
import com.auction.client.service.mock.MockManagementService;
import org.junit.jupiter.api.Test;

/** Kiểm tra các quy tắc quan trọng của dịch vụ mock. */
class MockManagementServiceTest {
    @Test
    void adminCannotLockOrDeleteSelf() {
        MockManagementService service = new MockManagementService(com.auction.client.service.mock.MockDataStore.createForTesting());
        assertThrows(IllegalStateException.class,
                () -> service.setLocked("U-admin_hieu", "U-admin_hieu", true));
        assertThrows(IllegalStateException.class,
                () -> service.deleteUser("U-admin_hieu", "U-admin_hieu"));
    }

    @Test
    void cancellationRequiresReasonAndOnlyAllowsOpenOrRunning() {
        MockManagementService service = new MockManagementService(com.auction.client.service.mock.MockDataStore.createForTesting());
        assertThrows(IllegalArgumentException.class, () -> service.cancelAuction("M01", " "));
        assertThrows(IllegalStateException.class, () -> service.cancelAuction("M03", "Lý do"));
        service.cancelAuction("M01", "Không hợp lệ");
        assertEquals(AuctionStatus.CANCELED, service.listAuctions().stream()
                .filter(a -> a.getId().equals("M01")).findFirst().orElseThrow().getStatus());
        assertThrows(IllegalStateException.class, () -> service.cancelAuction("M01", "Lần nữa"));
    }

    @Test
    void sellerCannotDeleteBidOnOrOtherSellerAuction() {
        MockManagementService service = new MockManagementService(com.auction.client.service.mock.MockDataStore.createForTesting());
        assertThrows(IllegalStateException.class, () -> service.deleteProduct("seller_hieu", "M02"));
        assertThrows(IllegalStateException.class, () -> service.deleteProduct("seller_other", "M01"));
        service.deleteProduct("seller_hieu", "M01");
        assertEquals(1, service.listProducts("seller_hieu").size());
    }
}
