package com.auction.client.service;

import static org.junit.jupiter.api.Assertions.*;
import com.auction.client.model.Item;
import com.auction.client.model.UserRole;
import com.auction.client.service.mock.MockAuctionClientService;
import org.junit.jupiter.api.Test;

/** Kiểm tra quyền đặt giá của người bán và khách. */
class MockAuctionBidAccessTest {
    @Test void guestAndSellerCannotBid() {
        var service = new MockAuctionClientService(com.auction.client.service.mock.MockDataStore.createForTesting());
        assertEquals("Vui lòng đăng nhập để đặt giá.", service.placeBid("AUC-101", 300_000_000).getMessage());
        assertTrue(service.login("seller_hieu", "123456", UserRole.SELLER));
        assertTrue(service.createItemAndAuction(new Item("OWN-1", "Sản phẩm", "Mô tả", "Khác", 1000, "seller_hieu"), 60, 100));
        var auction = service.getAuctions(null).stream().filter(a -> "OWN-1".equals(a.getItem().getId())).findFirst().orElseThrow();
        var result = service.placeBid(auction.getId(), 1100);
        assertFalse(result.isSuccess());
        assertEquals("Bạn không thể đặt giá cho sản phẩm của mình.", result.getMessage());
        assertEquals(1000, auction.getCurrentPrice());
    }
}
