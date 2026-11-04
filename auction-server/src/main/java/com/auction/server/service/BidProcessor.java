package com.auction.server.service;

import com.auction.server.exception.AuctionClosedException;
import com.auction.server.exception.InvalidBidException;
import com.auction.server.model.Auction;
import com.auction.server.model.Bid;
import com.auction.server.model.Bidder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Xử lý đặt giá cho các phiên đấu giá: kiểm tra hợp lệ, ném ngoại lệ,
 * chống bắn tỉa (anti-sniping), đấu giá tự động (auto-bidding) và chốt người thắng.
 *
 * <p>Mỗi phiên có một {@link ReentrantLock} riêng nên nhiều client đặt giá cùng lúc
 * vào cùng một phiên được xử lý tuần tự (không bị lost update), còn các phiên khác nhau
 * vẫn chạy song song.
 *
 * <p>AuctionService của Thành viên 2 chỉ cần gọi các hàm của lớp này.
 */
public class BidProcessor {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_FINISHED = "FINISHED";

    /** Trạng thái riêng của từng phiên: khóa, bộ máy auto-bid, danh sách người dùng auto-bid. */
    private static final class AuctionContext {
        private final ReentrantLock lock = new ReentrantLock();
        private final AutoBidEngine engine = new AutoBidEngine();
        private final Map<String, Bidder> autoBidders = new HashMap<>();
    }

    private final AntiSnipingPolicy antiSniping;
    private final Clock clock;
    private final ConcurrentMap<String, AuctionContext> contexts = new ConcurrentHashMap<>();

    public BidProcessor() {
        this(new AntiSnipingPolicy(), Clock.systemDefaultZone());
    }

    public BidProcessor(AntiSnipingPolicy antiSniping, Clock clock) {
        this.antiSniping = antiSniping;
        this.clock = clock;
    }

    /**
     * Đặt giá thủ công. Sau đó các auto-bid (nếu có) sẽ tự phản ứng.
     *
     * @return các bid được chấp nhận theo thứ tự: bid thủ công trước, bid tự động sau
     * @throws InvalidBidException    giá không hợp lệ
     * @throws AuctionClosedException phiên đã kết thúc hoặc hết giờ
     */
    public List<Bid> placeBid(Auction auction, Bidder bidder, double amount)
            throws InvalidBidException, AuctionClosedException {
        requireNotNull(auction, bidder);
        AuctionContext ctx = contextOf(auction);
        ctx.lock.lock();
        try {
            ensureBiddable(auction);
            validateAmount(auction, bidder, amount);

            List<Bid> placed = new ArrayList<>();
            placed.add(accept(auction, bidder, amount));
            applyAntiSniping(auction);
            placed.addAll(runAutoBids(auction, ctx));
            return placed;
        } finally {
            ctx.lock.unlock();
        }
    }

    /**
     * Đăng ký đấu giá tự động. Nếu người này chưa dẫn đầu và còn đủ ngân sách,
     * hệ thống đặt giá ngay lập tức.
     *
     * @return các bid tự động được sinh ra ngay (có thể rỗng)
     */
    public List<Bid> registerAutoBid(Auction auction, Bidder bidder, double maxBid, double increment)
            throws InvalidBidException, AuctionClosedException {
        requireNotNull(auction, bidder);
        AuctionContext ctx = contextOf(auction);
        ctx.lock.lock();
        try {
            ensureBiddable(auction);
            if (increment < auction.getStepPrice()) {
                throw new InvalidBidException(
                        "Bước giá tự động phải ≥ bước giá của phiên (" + auction.getStepPrice() + ")");
            }
            if (maxBid < minimumNextBid(auction)) {
                throw new InvalidBidException(
                        "Giá tối đa phải ≥ mức đặt tối thiểu hiện tại (" + minimumNextBid(auction) + ")");
            }
            ctx.engine.register(bidder.getId(), maxBid, increment);
            ctx.autoBidders.put(bidder.getId(), bidder);

            List<Bid> placed = runAutoBids(auction, ctx);
            if (!placed.isEmpty()) {
                applyAntiSniping(auction);
            }
            return placed;
        } finally {
            ctx.lock.unlock();
        }
    }

    /** Hủy đăng ký đấu giá tự động của một người. */
    public void cancelAutoBid(Auction auction, Bidder bidder) {
        requireNotNullUnchecked(auction, bidder);
        AuctionContext ctx = contextOf(auction);
        ctx.lock.lock();
        try {
            ctx.engine.unregister(bidder.getId());
            ctx.autoBidders.remove(bidder.getId());
        } finally {
            ctx.lock.unlock();
        }
    }

    /** Nếu phiên đã quá giờ kết thúc thì đóng lại. Trả về true nếu phiên đã/vừa được đóng. */
    public boolean closeIfExpired(Auction auction) {
        requireNotNullUnchecked(auction, null);
        AuctionContext ctx = contextOf(auction);
        ctx.lock.lock();
        try {
            if (STATUS_FINISHED.equals(auction.getStatus())) {
                return true;
            }
            if (!now().isBefore(auction.getEndTime())) {
                auction.setStatus(STATUS_FINISHED);
                return true;
            }
            return false;
        } finally {
            ctx.lock.unlock();
        }
    }

    /**
     * Chốt phiên đấu giá và trả về người thắng (rỗng nếu không có ai đặt giá).
     * Sau khi chốt, mọi lượt đặt giá mới sẽ bị từ chối.
     */
    public Optional<Bidder> finish(Auction auction) {
        requireNotNullUnchecked(auction, null);
        AuctionContext ctx = contextOf(auction);
        ctx.lock.lock();
        try {
            auction.setStatus(STATUS_FINISHED);
            if (auction.getBidHistory().isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(auction.getCurrentHighestBidder());
        } finally {
            ctx.lock.unlock();
            contexts.remove(auction.getAuctionId());
        }
    }

    // ------------------------------------------------------------------ nội bộ

    private AuctionContext contextOf(Auction auction) {
        return contexts.computeIfAbsent(auction.getAuctionId(), id -> new AuctionContext());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static void requireNotNull(Auction auction, Bidder bidder) throws InvalidBidException {
        if (auction == null) {
            throw new InvalidBidException("Thiếu thông tin phiên đấu giá");
        }
        if (bidder == null) {
            throw new InvalidBidException("Thiếu thông tin người đặt giá");
        }
    }

    private static void requireNotNullUnchecked(Auction auction, Bidder bidder) {
        if (auction == null) {
            throw new IllegalArgumentException("auction không được null");
        }
    }

    /** Phiên chỉ nhận giá khi đang OPEN/RUNNING và chưa quá giờ kết thúc. */
    private void ensureBiddable(Auction auction) throws AuctionClosedException {
        String status = auction.getStatus();
        if (!STATUS_OPEN.equals(status) && !STATUS_RUNNING.equals(status)) {
            throw new AuctionClosedException("Phiên đấu giá đã đóng (trạng thái: " + status + ")");
        }
        if (!now().isBefore(auction.getEndTime())) {
            auction.setStatus(STATUS_FINISHED);
            throw new AuctionClosedException("Phiên đấu giá đã hết giờ");
        }
    }

    /** Mức giá tối thiểu cho lượt đặt tiếp theo. */
    private static double minimumNextBid(Auction auction) {
        if (auction.getBidHistory().isEmpty()) {
            return auction.getStartingPrice();
        }
        return auction.getCurrentPrice() + auction.getStepPrice();
    }

    private static void validateAmount(Auction auction, Bidder bidder, double amount)
            throws InvalidBidException {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new InvalidBidException("Giá đặt phải là số dương");
        }
        Bidder leader = auction.getCurrentHighestBidder();
        if (leader != null && !auction.getBidHistory().isEmpty()
                && leader.getId().equals(bidder.getId())) {
            throw new InvalidBidException("Bạn đang là người dẫn đầu phiên này");
        }
        double minimum = minimumNextBid(auction);
        if (amount < minimum) {
            throw new InvalidBidException(
                    "Giá đặt phải từ " + minimum + " trở lên (giá hiện tại: "
                            + auction.getCurrentPrice() + ", bước giá: " + auction.getStepPrice() + ")");
        }
    }

    private Bid accept(Auction auction, Bidder bidder, double amount) {
        Bid bid = new Bid("BID-" + UUID.randomUUID(), bidder, amount);
        auction.addBid(bid);
        auction.setStatus(STATUS_RUNNING);
        return bid;
    }

    private void applyAntiSniping(Auction auction) {
        auction.setEndTime(antiSniping.computeNewEndTime(auction.getEndTime(), now()));
    }

    private List<Bid> runAutoBids(Auction auction, AuctionContext ctx) {
        Bidder leader = auction.getCurrentHighestBidder();
        String leaderId = (leader == null || auction.getBidHistory().isEmpty()) ? null : leader.getId();
        List<Bid> placed = new ArrayList<>();
        for (AutoBidEngine.PlacedBid pb : ctx.engine.respondTo(auction.getCurrentPrice(), leaderId)) {
            Bidder bidder = ctx.autoBidders.get(pb.bidderId());
            if (bidder != null) {
                placed.add(accept(auction, bidder, pb.amount()));
            }
        }
        return placed;
    }
}
