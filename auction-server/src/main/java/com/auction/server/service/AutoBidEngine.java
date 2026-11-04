package com.auction.server.service;

import com.auction.server.exception.InvalidBidException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Bộ máy đấu giá tự động (auto-bidding) cho MỘT phiên đấu giá.
 *
 * <p>Mỗi người đăng ký cung cấp {@code maxBid} (giá tối đa chịu trả) và
 * {@code increment} (bước giá). Khi giá thay đổi, {@link #respondTo} mô phỏng
 * chuỗi bid tự động cho tới khi không ai còn đủ ngân sách để vượt giá.
 * Nếu nhiều người cùng đủ điều kiện, người đăng ký SỚM HƠN được ưu tiên
 * (dùng {@link PriorityQueue}).
 *
 * <p>Các phương thức đều {@code synchronized} để an toàn khi nhiều luồng dùng chung.
 */
public class AutoBidEngine {

    /** Giới hạn số vòng để tránh vòng lặp quá dài khi bước giá rất nhỏ. */
    private static final int MAX_ROUNDS = 100_000;

    /** Đăng ký auto-bid. {@code order} là thứ tự đăng ký (nhỏ hơn = sớm hơn). */
    public record AutoBidder(String bidderId, double maxBid, double increment, long order) { }

    /** Một lượt bid do hệ thống sinh ra. */
    public record PlacedBid(String bidderId, double amount) { }

    private final List<AutoBidder> registered = new ArrayList<>();
    private long counter = 0;

    /** Đăng ký (hoặc đăng ký lại) auto-bid. Đăng ký lại sẽ mất ưu tiên thời gian cũ. */
    public synchronized void register(String bidderId, double maxBid, double increment)
            throws InvalidBidException {
        if (bidderId == null || bidderId.isBlank()) {
            throw new InvalidBidException("Thiếu mã người đặt giá");
        }
        if (!(maxBid > 0) || Double.isInfinite(maxBid)) {
            throw new InvalidBidException("Giá tối đa phải lớn hơn 0");
        }
        if (!(increment > 0) || Double.isInfinite(increment)) {
            throw new InvalidBidException("Bước giá phải lớn hơn 0");
        }
        if (increment > maxBid) {
            throw new InvalidBidException("Bước giá không được lớn hơn giá tối đa");
        }
        registered.removeIf(a -> a.bidderId().equals(bidderId));
        registered.add(new AutoBidder(bidderId, maxBid, increment, ++counter));
    }

    public synchronized void unregister(String bidderId) {
        registered.removeIf(a -> a.bidderId().equals(bidderId));
    }

    public synchronized int size() {
        return registered.size();
    }

    /**
     * Mô phỏng các bid tự động sau khi giá hiện tại là {@code currentPrice}
     * và người đang dẫn đầu là {@code leaderId} (có thể null nếu chưa có ai).
     *
     * @return các bid được sinh ra theo thứ tự (rỗng nếu không ai phản ứng).
     *         Giá cuối = amount của phần tử cuối; người dẫn đầu = bidderId của nó.
     */
    public synchronized List<PlacedBid> respondTo(double currentPrice, String leaderId) {
        List<PlacedBid> placed = new ArrayList<>();
        double price = currentPrice;
        String leader = leaderId;

        for (int round = 0; round < MAX_ROUNDS; round++) {
            PriorityQueue<AutoBidder> candidates =
                    new PriorityQueue<>(Comparator.comparingLong(AutoBidder::order));
            for (AutoBidder a : registered) {
                boolean notLeader = !a.bidderId().equals(leader);
                boolean canAfford = a.maxBid() >= price + a.increment();
                if (notLeader && canAfford) {
                    candidates.add(a);
                }
            }
            AutoBidder next = candidates.poll();
            if (next == null) {
                break;
            }
            price += next.increment();
            leader = next.bidderId();
            placed.add(new PlacedBid(leader, price));
        }
        return placed;
    }
}
