package com.auction.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.server.exception.InvalidBidException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AutoBidEngineTest {

    @Test
    void noRegistration_noResponse() {
        AutoBidEngine engine = new AutoBidEngine();
        assertTrue(engine.respondTo(100, "manual").isEmpty());
    }

    @Test
    void singleAutoBidder_outbidsManualByIncrement() throws InvalidBidException {
        AutoBidEngine engine = new AutoBidEngine();
        engine.register("A", 500, 10);

        List<AutoBidEngine.PlacedBid> result = engine.respondTo(100, "manual");

        assertEquals(1, result.size());
        assertEquals("A", result.get(0).bidderId());
        assertEquals(110, result.get(0).amount());
    }

    @Test
    void leaderDoesNotOutbidItself() throws InvalidBidException {
        AutoBidEngine engine = new AutoBidEngine();
        engine.register("A", 500, 10);
        assertTrue(engine.respondTo(100, "A").isEmpty());
    }

    @Test
    void cannotAffordIncrement_noResponse() throws InvalidBidException {
        AutoBidEngine engine = new AutoBidEngine();
        engine.register("A", 105, 10);
        assertTrue(engine.respondTo(100, "manual").isEmpty());
    }

    @Test
    void twoAutoBidders_higherMaxWins() throws InvalidBidException {
        AutoBidEngine engine = new AutoBidEngine();
        engine.register("A", 100, 5);
        engine.register("B", 200, 10);

        List<AutoBidEngine.PlacedBid> result = engine.respondTo(50, "manual");
        AutoBidEngine.PlacedBid last = result.get(result.size() - 1);

        assertEquals("B", last.bidderId());
        assertEquals(110, last.amount());
    }

    @Test
    void sameMaxAndIncrement_earlierRegistrationWins() throws InvalidBidException {
        AutoBidEngine engine = new AutoBidEngine();
        engine.register("A", 100, 10);
        engine.register("B", 100, 10);

        List<AutoBidEngine.PlacedBid> result = engine.respondTo(50, "manual");
        AutoBidEngine.PlacedBid last = result.get(result.size() - 1);

        assertEquals("A", last.bidderId());
        assertEquals(100, last.amount());
    }

    @Test
    void unregister_removesBidder() throws InvalidBidException {
        AutoBidEngine engine = new AutoBidEngine();
        engine.register("A", 500, 10);
        engine.unregister("A");
        assertEquals(0, engine.size());
        assertTrue(engine.respondTo(100, "manual").isEmpty());
    }

    @Test
    void invalidRegistration_throws() {
        AutoBidEngine engine = new AutoBidEngine();
        assertThrows(InvalidBidException.class, () -> engine.register("A", 0, 10));
        assertThrows(InvalidBidException.class, () -> engine.register("A", 100, 0));
        assertThrows(InvalidBidException.class, () -> engine.register("A", 100, 200));
        assertThrows(InvalidBidException.class, () -> engine.register(" ", 100, 10));
        assertThrows(InvalidBidException.class, () -> engine.register("A", Double.NaN, 10));
    }
}
