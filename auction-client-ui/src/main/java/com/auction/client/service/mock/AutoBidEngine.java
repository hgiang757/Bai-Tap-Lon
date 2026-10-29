package com.auction.client.service.mock;

import com.auction.client.model.*;
import java.time.LocalDateTime;
import java.util.*;

/** Bộ máy đấu giá tự động độc lập giao diện; gọi khi đang khóa kho. */
public final class AutoBidEngine {
    private static final int MAX_ROUNDS = 10000;
    private final MockDataStore store;
    /** Tạo bộ máy trên kho mô phỏng. */
    public AutoBidEngine(MockDataStore store) { this.store = store; }
    /** Xử lý những lượt tự động có thể thực hiện trong cùng khóa dữ liệu. */
    public void process(Auction auction) { synchronized (store) {
        if (auction.getStatus() != AuctionStatus.RUNNING) return;
        var queue = store.autoQueue(auction.getId());
        for (int round = 0; round < MAX_ROUNDS; round++) {
            List<AutoBid> ordered = new ArrayList<>(queue);
            ordered.sort(Comparator.comparing(AutoBid::getCreatedAt).thenComparing(AutoBid::getId));
            AutoBid selected = null;
            long next = 0;
            for (AutoBid rule : ordered) {
                if (!rule.isActive() || rule.getUsername().equalsIgnoreCase(auction.getHighestBidderUsername())) continue;
                double minimum = auction.getCurrentPrice() + auction.getMinBidStep();
                if (minimum > rule.getMaxBid()) { rule.reachLimit(); continue; }
                double desired = Math.max(minimum, auction.getCurrentPrice() + rule.getIncrement());
                long price = (long) Math.min(rule.getMaxBid(), desired);
                // Nếu cùng trần, người đăng ký sớm được ưu tiên ở mức trần.
                boolean losesTie = price == rule.getMaxBid() && ordered.stream().anyMatch(other -> other != rule
                        && other.isActive() && other.getMaxBid() == rule.getMaxBid()
                        && (other.getCreatedAt().isBefore(rule.getCreatedAt())
                            || (other.getCreatedAt().equals(rule.getCreatedAt()) && other.getId().compareTo(rule.getId()) < 0)));
                if (losesTie) { rule.reachLimit(); continue; }
                MockDataStore.Hold hold = store.holds.get(auction.getId());
                long ownHold = hold != null && hold.username().equals(rule.getUsername()) ? hold.amount() : 0;
                if (store.balance(rule.getUsername()) - store.held(rule.getUsername()) + ownHold < price) continue;
                selected = rule; next = price; break;
            }
            if (selected == null) return;
            if (!store.holdBid(auction.getId(), selected.getUsername(), next)) return;
            auction.setCurrentPrice(next); auction.setHighestBidderUsername(selected.getUsername());
            auction.setTotalBids(auction.getTotalBids()+1); selected.recordBid();
            if (next >= selected.getMaxBid()) selected.reachLimit();
            Bid bid = new Bid("BID-" + UUID.randomUUID(), auction.getId(), selected.getUsername(), next, LocalDateTime.now(), true);
            store.bidHistories.computeIfAbsent(auction.getId(), key -> new ArrayList<>()).add(bid);
            store.notifyBid(auction.getId(), bid);
        }
    } }
}
