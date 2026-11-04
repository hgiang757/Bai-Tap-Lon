package com.auction.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AntiSnipingPolicyTest {

    private final AntiSnipingPolicy policy =
            new AntiSnipingPolicy(Duration.ofSeconds(30), Duration.ofSeconds(60));
    private final LocalDateTime end = LocalDateTime.of(2026, 11, 10, 10, 0, 0);

    @Test
    void bidFarFromEnd_doesNotExtend() {
        LocalDateTime bidTime = end.minusSeconds(300);
        assertFalse(policy.shouldExtend(end, bidTime));
        assertEquals(end, policy.computeNewEndTime(end, bidTime));
    }

    @Test
    void bidInLastSeconds_extends() {
        LocalDateTime bidTime = end.minusSeconds(10);
        assertTrue(policy.shouldExtend(end, bidTime));
        assertEquals(end.plusSeconds(60), policy.computeNewEndTime(end, bidTime));
    }

    @Test
    void bidExactlyAtThreshold_extends() {
        assertTrue(policy.shouldExtend(end, end.minusSeconds(30)));
    }

    @Test
    void bidAfterEnd_doesNotExtend() {
        assertFalse(policy.shouldExtend(end, end.plusSeconds(1)));
        assertFalse(policy.shouldExtend(end, end));
    }

    @Test
    void invalidConfig_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new AntiSnipingPolicy(Duration.ZERO, Duration.ofSeconds(60)));
        assertThrows(IllegalArgumentException.class,
                () -> new AntiSnipingPolicy(Duration.ofSeconds(30), Duration.ofSeconds(-1)));
    }
}
