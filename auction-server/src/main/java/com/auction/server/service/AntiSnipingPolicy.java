package com.auction.server.service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Chính sách chống "bắn tỉa" (anti-sniping).
 *
 * <p>Nếu có bid hợp lệ trong khoảng {@code threshold} cuối của phiên,
 * thời gian kết thúc được cộng thêm {@code extension}.
 * Lớp không giữ trạng thái nên an toàn khi dùng từ nhiều luồng.
 */
public final class AntiSnipingPolicy {

    public static final Duration DEFAULT_THRESHOLD = Duration.ofSeconds(30);
    public static final Duration DEFAULT_EXTENSION = Duration.ofSeconds(60);

    private final Duration threshold;
    private final Duration extension;

    public AntiSnipingPolicy() {
        this(DEFAULT_THRESHOLD, DEFAULT_EXTENSION);
    }

    public AntiSnipingPolicy(Duration threshold, Duration extension) {
        if (threshold == null || threshold.isNegative() || threshold.isZero()) {
            throw new IllegalArgumentException("threshold phải > 0");
        }
        if (extension == null || extension.isNegative() || extension.isZero()) {
            throw new IllegalArgumentException("extension phải > 0");
        }
        this.threshold = threshold;
        this.extension = extension;
    }

    /** True nếu bid tại {@code bidTime} nằm trong khoảng cuối phiên (và phiên chưa hết giờ). */
    public boolean shouldExtend(LocalDateTime endTime, LocalDateTime bidTime) {
        Duration remaining = Duration.between(bidTime, endTime);
        return !remaining.isNegative()
                && !remaining.isZero()
                && remaining.compareTo(threshold) <= 0;
    }

    /** Trả về thời gian kết thúc mới (bằng {@code endTime} nếu không cần gia hạn). */
    public LocalDateTime computeNewEndTime(LocalDateTime endTime, LocalDateTime bidTime) {
        return shouldExtend(endTime, bidTime) ? endTime.plus(extension) : endTime;
    }

    public Duration getThreshold() {
        return threshold;
    }

    public Duration getExtension() {
        return extension;
    }
}
