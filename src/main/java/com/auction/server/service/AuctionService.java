package com.auction.server.service;

import com.auction.server.dao.AuctionRepository;
import com.auction.server.dao.ItemRepository;
import com.auction.server.dao.UserRepository;
import com.auction.server.exception.ApiException;
import com.auction.server.model.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {
    // 1. SINGLETON PATTERN (Theo yêu cầu Tuần 6)
    private static volatile AuctionService instance;

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ItemService itemService;
    private final AntiSnipingPolicy antiSnipingPolicy;

    // 2. CONCURRENCY LOCKS (Chống Race Condition khi nhiều người đặt giá cùng lúc)
    private final Map<String, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    // 3. OBSERVER PATTERN (Danh sách các Listener nhận thông báo Realtime)
    private final List<AuctionUpdateListener> listeners = new CopyOnWriteArrayList<>();

    public AuctionService() {
        this(new AuctionRepository(), new UserRepository(), new ItemRepository());
    }

    public AuctionService(
            AuctionRepository auctionRepository,
            UserRepository userRepository,
            ItemRepository itemRepository) {
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.userService = new UserService(userRepository, auctionRepository);
        this.itemService = new ItemService(itemRepository);
        this.antiSnipingPolicy = new AntiSnipingPolicy();
    }

    public static AuctionService getInstance() {
        if (instance == null) {
            synchronized (AuctionService.class) {
                if (instance == null) {
                    instance = new AuctionService();
                }
            }
        }
        return instance;
    }

    // Đăng ký / Hủy đăng ký Observer
    public void addListener(AuctionUpdateListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(AuctionUpdateListener listener) {
        listeners.remove(listener);
    }

    // Tạo phiên đấu giá mới
    public Auction createAuction(
            String sellerId,
            String itemType,
            String itemName,
            String itemDescription,
            double startingPrice,
            double minIncrement,
            Instant startTime,
            Instant endTime) {

        User seller = userService.getUserOrThrow(sellerId);
        if (seller.getRole() != UserRole.SELLER && seller.getRole() != UserRole.ADMIN) {
            throw new ApiException(403, "FORBIDDEN", "Chỉ Seller hoặc Admin mới được tạo phiên đấu giá");
        }

        Item item = itemService.createItem(itemType, itemName, itemDescription, startingPrice, "", "");
        String auctionId = "AUC-" + UUID.randomUUID().toString().substring(0, 8);

        Auction auction = new Auction(
                auctionId,
                item,
                sellerId,
                startTime != null ? startTime : Instant.now(),
                endTime != null ? endTime : Instant.now().plusSeconds(3600),
                minIncrement);

        return auctionRepository.save(auction);
    }

    // Đặt giá (Xử lý đồng thời bằng ReentrantLock trên từng phiên đấu giá)
    public Bid placeBid(String auctionId, String bidderId, double amount) {
        ReentrantLock lock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock(true));
        lock.lock();
        try {
            Auction auction = getAuctionOrThrow(auctionId);
            User user = userService.getUserOrThrow(bidderId);
            Instant now = Instant.now();

            // Tự động đóng phiên nếu đã hết giờ
            if (auction.getEndTime() != null && !now.isBefore(auction.getEndTime())) {
                closeAuctionInternal(auction);
                // TODO (TV4): Thay ApiException bằng AuctionClosedException
                throw new ApiException(400, "AUCTION_CLOSED", "Phiên đấu giá đã kết thúc");
            }

            if (!auction.isRunning(now)) {
                // TODO (TV4): Thay ApiException bằng AuctionClosedException
                throw new ApiException(400, "AUCTION_NOT_RUNNING", "Phiên đấu giá không ở trạng thái hoạt động");
            }

            if (bidderId.equals(auction.getSellerId())) {
                // TODO (TV4): Thay ApiException bằng InvalidBidException
                throw new ApiException(400, "SELF_BIDDING", "Người bán không được tự đặt giá sản phẩm của mình");
            }

            double minRequired = auction.getMinimumNextBid();
            if (amount < minRequired) {
                // TODO (TV4): Thay ApiException bằng InvalidBidException
                throw new ApiException(
                        400,
                        "INVALID_BID_AMOUNT",
                        "Giá đặt phải tối thiểu từ " + minRequired + " trở lên");
            }

            if (user.getBalance() < amount) {
                // TODO (TV4): Thay ApiException bằng InvalidBidException
                throw new ApiException(400, "INSUFFICIENT_BALANCE", "Số dư ví không đủ để đặt mức giá này");
            }

            // Ghi nhận lượt đặt giá mới
            Bid newBid = new Bid(auctionId, bidderId, user.getUsername(), amount);
            auction.recordBid(newBid);

            // Kiểm tra Anti-Sniping (Gia hạn nếu đặt giá sát giờ kết thúc)
            antiSnipingPolicy.applyIfNeeded(auction, now);

            auctionRepository.save(auction);

            // Thông báo Realtime cho tất cả Observer (Socket Server)
            notifyNewBid(auction, newBid);

            return newBid;
        } finally {
            lock.unlock();
        }
    }

    // Kết thúc phiên đấu giá và thanh toán cho người thắng
    public Auction closeAuction(String auctionId) {
        ReentrantLock lock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock(true));
        lock.lock();
        try {
            Auction auction = getAuctionOrThrow(auctionId);
            return closeAuctionInternal(auction);
        } finally {
            lock.unlock();
        }
    }

    private Auction closeAuctionInternal(Auction auction) {
        if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID) {
            return auction;
        }

        auction.setStatus(AuctionStatus.FINISHED);
        Bid winningBid = auction.getHighestBid();

        if (winningBid != null) {
            userRepository.findById(winningBid.getBidderId()).ifPresent(winner -> {
                if (winner.getBalance() >= winningBid.getAmount()) {
                    winner.deduct(winningBid.getAmount());
                    if (winner instanceof Bidder bidder) {
                        bidder.incrementWonAuctions();
                    }
                    userRepository.save(winner);

                    // Cộng tiền cho Seller
                    userRepository.findById(auction.getSellerId()).ifPresent(seller -> {
                        seller.deposit(winningBid.getAmount());
                        userRepository.save(seller);
                    });

                    auction.setStatus(AuctionStatus.PAID);
                    auctionRepository.recordTransaction(new Transaction(
                            winner.getId(),
                            auction.getId(),
                            winningBid.getAmount(),
                            "BID_PAYMENT",
                            "Thanh toán thắng phiên đấu giá " + auction.getName()));
                }
            });
        }

        Auction saved = auctionRepository.save(auction);
        notifyStatusChanged(saved);
        return saved;
    }

    public Auction getAuctionOrThrow(String auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ApiException(404, "AUCTION_NOT_FOUND", "Không tìm thấy phiên đấu giá: " + auctionId));
    }

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    public UserService getUserService() {
        return userService;
    }

    public ItemService getItemService() {
        return itemService;
    }

    private void notifyNewBid(Auction auction, Bid bid) {
        for (AuctionUpdateListener listener : listeners) {
            listener.onNewBid(auction, bid);
        }
    }

    private void notifyStatusChanged(Auction auction) {
        for (AuctionUpdateListener listener : listeners) {
            listener.onAuctionStatusChanged(auction);
        }
    }
}
