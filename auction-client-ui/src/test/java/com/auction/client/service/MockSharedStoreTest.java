package com.auction.client.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import com.auction.client.model.*;
import com.auction.client.service.mock.*;
import org.junit.jupiter.api.Test;

/** Kiểm tra dữ liệu sản phẩm và phiên được chia sẻ giữa hai dịch vụ. */
class MockSharedStoreTest {
    @Test void productChangesAndStatusesAreVisibleToAuctionService() {
        var store = MockDataStore.createForTesting();
        var products = new MockManagementService(store);
        var auctions = new MockAuctionClientService(store);
        var now = LocalDateTime.now();
        var item = new Item("I-NEW", "Xe mới", "Mô tả", "Vehicle", 1000, "seller_hieu");
        var auction = new Auction("NEW", item, "seller_hieu", 1000, null, AuctionStatus.OPEN, now.plusHours(1), now.plusHours(2), 100);
        products.saveProduct(auction, null);
        assertEquals("Xe cộ", auctions.getAuctions(null).stream().filter(a -> a.getId().equals("NEW")).findFirst().orElseThrow().getItemCategory());
        var changed = new Auction("NEW", new Item("I-NEW", "Xe đã sửa", "Mô tả", "Vehicle", 1200, "seller_hieu"), "seller_hieu", 1200, null, AuctionStatus.OPEN, now.plusHours(1), now.plusHours(2), 100);
        products.saveProduct(changed, null);
        assertEquals("Xe đã sửa", auctions.getAuctionById("NEW").getItemName());
        products.deleteProduct("seller_hieu", "NEW");
        assertNull(auctions.getAuctionById("NEW"));
        var started = new Auction("STARTED", item, "seller_hieu", 1000, null, AuctionStatus.OPEN, now.minusMinutes(1), now.plusHours(1), 100);
        products.saveProduct(started, null);
        assertEquals(AuctionStatus.RUNNING, auctions.getAuctionById("STARTED").getStatus());
        var expired = new Auction("EXPIRED", item, "seller_hieu", 1000, null, AuctionStatus.OPEN, now.minusHours(2), now.minusHours(1), 100);
        products.saveProduct(expired, null);
        assertEquals(AuctionStatus.FINISHED, auctions.getAuctions(null).stream().filter(a -> a.getId().equals("EXPIRED")).findFirst().orElseThrow().getStatus());
        assertEquals(AuctionStatus.PAID, auctions.getAuctionById("M05").getStatus());
    }
    @Test void categoriesRecognizeLegacyAndCurrentNames() {
        assertEquals(ItemCategory.ART, ItemCategory.from("Art"));
        assertEquals(ItemCategory.ELECTRONICS, ItemCategory.from("Điện tử"));
        assertEquals(ItemCategory.VEHICLE, ItemCategory.from("Vehicle"));
        assertEquals(ItemCategory.JEWELRY, ItemCategory.from("Đồng hồ cao cấp"));
        assertEquals(ItemCategory.REAL_ESTATE, ItemCategory.from("Bất động sản & Khác"));
    }
}
