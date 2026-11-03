package com.auction.client.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.auction.client.service.BidSubmissionTracker.Phase.*;

class BidSubmissionTrackerTest {
    private final BidSubmissionTracker tracker = new BidSubmissionTracker();
    private final BidSubmissionTracker.Key key = new BidSubmissionTracker.Key("alice", "A");

    @Test void timeoutStaysUnknownEvenAfterSuccessfulFreshHistoryRead() {
        var ticket = tracker.begin(key);
        tracker.uncertain(ticket);
        tracker.refreshed(tracker.reading(key));
        assertEquals(UNKNOWN, tracker.phase(key));
        assertThrows(IllegalStateException.class, () -> tracker.begin(key));
    }

    @Test void readStartedBeforeCommandOrBeforeResponseCannotUnlock() {
        var beforeCommand = tracker.reading(key);
        var ticket = tracker.begin(key);
        var duringCommand = tracker.reading(key);
        tracker.answered(ticket);
        assertFalse(tracker.isCurrent(beforeCommand));
        assertFalse(tracker.isCurrent(duringCommand));
        tracker.refreshed(beforeCommand); tracker.refreshed(duringCommand);
        assertEquals(AWAITING_REFRESH, tracker.phase(key));
        tracker.refreshed(tracker.reading(key));
        assertEquals(READY, tracker.phase(key));
    }

    @Test void failedReadDoesNotUnlockAndOldCommandCannotChangeNewCommand() {
        var first = tracker.begin(key); tracker.answered(first);
        // Không có refreshed() vì lần đọc lỗi.
        assertEquals(AWAITING_REFRESH, tracker.phase(key));
        tracker.refreshed(tracker.reading(key));
        var second = tracker.begin(key);
        tracker.uncertain(first); tracker.notSubmitted(first);
        assertEquals(SENDING, tracker.phase(key));
        tracker.uncertain(second);
        assertEquals(UNKNOWN, tracker.phase(key));
    }

    @Test void unknownIsIsolatedByUserAndAuctionAndOnlyQueueRejectionMeansNotSent() {
        var ticket = tracker.begin(key); tracker.uncertain(ticket);
        assertEquals(READY, tracker.phase(new BidSubmissionTracker.Key("alice", "B")));
        assertEquals(READY, tracker.phase(new BidSubmissionTracker.Key("bob", "A")));
        tracker.notSubmitted(ticket); // Sau khi lỗi trong work, không thể đổi ý coi là chưa gửi.
        assertEquals(UNKNOWN, tracker.phase(key));
        var other = new BidSubmissionTracker.Key("bob", "B");
        tracker.notSubmitted(tracker.begin(other));
        assertEquals(READY, tracker.phase(other));
    }
}
