package com.auction.client.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuctionTimeTest {
    @Test void parsesStrictTime() {
        assertEquals(LocalTime.of(14, 30), AuctionTime.parse("14:30"));
        assertNull(AuctionTime.parse("24:00")); assertNull(AuctionTime.parse("9:30"));
    }
    @Test void summarizesDuration() {
        var a = LocalDateTime.of(2026, 10, 29, 12, 30);
        assertEquals("Phiên kéo dài 1 ngày 2 giờ, kết thúc lúc 30/10/2026 14:30.", AuctionTime.summary(a, a.plusDays(1).plusHours(2)));
    }
    @Test void validatesBounds() {
        var now = LocalDateTime.of(2026, 10, 29, 12, 0);
        assertTrue(AuctionTime.valid(now, now.plusMinutes(1), now));
        assertFalse(AuctionTime.valid(now.minusMinutes(1), now.plusMinutes(1), now));
        assertFalse(AuctionTime.valid(now, now, now));
    }
}
