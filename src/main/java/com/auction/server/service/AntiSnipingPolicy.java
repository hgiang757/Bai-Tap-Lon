package com.auction.server.service;

import com.auction.server.model.Auction;
import java.time.Duration;
import java.time.Instant;

public class AntiSnipingPolicy {
    private final Duration triggerWindow;
    private final Duration extensionDuration;

    public AntiSnipingPolicy() {
        this(Duration.ofSeconds(30), Duration.ofSeconds(60));
    }

    public AntiSnipingPolicy(Duration triggerWindow, Duration extensionDuration) {
        this.triggerWindow = triggerWindow;
        this.extensionDuration = extensionDuration;
    }

    public boolean applyIfNeeded(Auction auction, Instant bidTime) {
        Instant endTime = auction.getEndTime();
        if (endTime == null || !bidTime.isBefore(endTime)) {
            return false;
        }
        Duration remaining = Duration.between(bidTime, endTime);
        if (remaining.compareTo(triggerWindow) <= 0) {
            auction.extendEndTime(extensionDuration.getSeconds());
            return true;
        }
        return false;
    }
}
