package com.auction.server.dao;

import static com.auction.shared.JsonProtocol.obj;
import static org.junit.jupiter.api.Assertions.*;

import com.auction.server.model.Auction;
import com.auction.server.model.BidTransaction;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuctionJsonMapperTest {
    @Test
    void readsLegacyAuctionWithoutMutatingHistoryImagesOrExtensionFields() {
        JsonObject legacy = JsonParser.parseString("""
                {"id":"a1","sellerId":"s1","currentPrice":1200000,"minBidStep":100000,
                 "status":"RUNNING","startTime":"2026-11-12T10:00:00",
                 "endTime":"2026-11-12T11:00:00","settled":false,"deleted":false,
                 "highestBidderId":"b1","totalBids":1,"version":17,"imageBase64":"legacy-image",
                 "bids":[{"id":"old-bid","amount":1200000}],"futureField":{"preserve":true}}
                """).getAsJsonObject();
        JsonObject before = legacy.deepCopy();
        Auction auction = AuctionJsonMapper.auction(legacy);
        assertEquals("a1", auction.id());
        assertEquals("s1", auction.sellerId());
        assertEquals(1_300_000, auction.minimumNextBid());
        assertDoesNotThrow(() -> auction.requireBidder("b2", "BIDDER", auction.startTime()));
        assertEquals(before, legacy);
    }

    @Test
    void applyingDomainBidUpdatesOnlyTheLeaderPriceAndCount() {
        JsonObject original = obj("id", "a", "sellerId", "s", "currentPrice", 1_000_000,
                "minBidStep", 100_000, "status", "RUNNING", "highestBidderId", "",
                "highestBidderUsername", "", "totalBids", 0, "settled", false,
                "startTime", "2026-11-12T10:00", "endTime", "2026-11-12T11:00",
                "futureField", obj("preserve", true), "imageBase64", "original-image");
        JsonObject target = original.deepCopy();
        Auction accepted = AuctionJsonMapper.auction(target).withBid("b", 1_100_001);
        AuctionJsonMapper.applyBid(target, accepted, "buyer");
        JsonObject expected = original.deepCopy();
        expected.addProperty("highestBidderId", "b");
        expected.addProperty("highestBidderUsername", "buyer");
        expected.addProperty("currentPrice", 1_100_001);
        expected.addProperty("totalBids", 1);
        assertEquals(expected, target);
        assertEquals(accepted, AuctionJsonMapper.auction(target));
        assertEquals(1_000_000, original.get("currentPrice").getAsLong());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void writesExactLegacyBidFieldsAndIntegerMoney(boolean automatic) {
        LocalDateTime time = LocalDateTime.of(2026, 11, 12, 10, 30);
        BidTransaction bid = new BidTransaction("bid-id", "auction-id", "người_mua", 1_200_000, time, automatic);
        JsonObject expected = obj("id", "bid-id", "auctionId", "auction-id",
                "bidderUsername", "người_mua", "amount", 1_200_000,
                "timestamp", "2026-11-12T10:30", "auto", automatic);
        assertEquals(expected, AuctionJsonMapper.bid(bid));
    }
}
