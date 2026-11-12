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
                 "highestBidderId":"b1","version":17,"imageBase64":"legacy-image",
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
