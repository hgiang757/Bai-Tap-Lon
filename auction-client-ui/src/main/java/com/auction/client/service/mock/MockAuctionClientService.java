package com.auction.client.service.mock;

import com.auction.client.service.AuctionClientService;
import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.Bid;
import com.auction.client.model.Item;
import com.auction.client.model.User;
import com.auction.client.model.UserRole;
import com.auction.client.service.BidResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Triển khai Mock của AuctionClientService phục vụ phát triển giao diện độc lập.
 * Mô phỏng đầy đủ dữ liệu người dùng, sản phẩm, phiên đấu giá và lịch sử bid realtime.
 */
public class MockAuctionClientService implements AuctionClientService {

    private static final String DEMO_PASSWORD = "123456";
    private static final String BIDDER_USERNAME = "bidder_hieu";
    private static final String SELLER_USERNAME = "seller_hieu";
    private User currentUser;
    private final List<Auction> auctions = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final Map<String, String> registeredPasswords = new ConcurrentHashMap<>();
    private final Map<String, User> registeredUsers = new ConcurrentHashMap<>();
    private final Map<String, List<Bid>> bidHistories = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<Bid>>> listeners = new ConcurrentHashMap<>();

    public MockAuctionClientService() {
        initSampleData();
    }

    private void initSampleData() {
        // Thiết lập người dùng mặc định
        currentUser = new User("U001", "bidder_hieu", "Mai Đức Hiệu", "hieu@auction.vn", UserRole.BIDDER, 50_000_000.0);

        // Tạo danh mục sản phẩm mẫu
        Item item1 = new Item("IT01", "Đồng hồ Rolex Submariner Date", "Tình trạng như mới 99%, đầy đủ hộp sổ thẻ chính hãng.", "Đồng hồ cao cấp", 250_000_000.0, "seller_vip");
        Item item2 = new Item("IT02", "Tranh sơn mài Cổ Đồ", "Tác phẩm thủ công độc bản từ nghệ nhân Hà Nội thế kỷ 20.", "Nghệ thuật & Cổ vật", 45_000_000.0, "seller_art");
        Item item3 = new Item("IT03", "MacBook Pro M4 Max 64GB 1TB", "Máy mới nguyên seal, bảo hành Apple Care+ 3 năm.", "Thiết bị công nghệ", 75_000_000.0, "seller_tech");
        Item item4 = new Item("IT04", "Tượng Phật đồng thời Lý", "Hiện vật cổ có giấy chứng nhận thẩm định bảo tàng.", "Cổ vật quý", 120_000_000.0, "seller_art");
        Item item5 = new Item("IT05", "Vòng cổ ngọc trai thiên nhiên", "Ngọc trai tuyển chọn, kèm giấy kiểm định chất lượng.", "Trang sức & Đá quý", 35_000_000.0, SELLER_USERNAME);
        Item item6 = new Item("IT06", "Căn hộ ven sông Thảo Điền", "Căn hộ đã hoàn thiện nội thất, pháp lý đầy đủ.", "Bất động sản & Khác", 4_500_000_000.0, "seller_home");
        Item item7 = new Item("IT07", "Sedan hạng sang đời 2023", "Xe đã kiểm định, lịch sử bảo dưỡng đầy đủ.", "Xe cộ & Phụ tùng", 1_200_000_000.0, "seller_auto");

        items.add(item1);
        items.add(item2);
        items.add(item3);
        items.add(item4);
        items.add(item5);
        items.add(item6);
        items.add(item7);

        // Tạo các phiên đấu giá mẫu với các trạng thái khác nhau
        Auction auction1 = new Auction("AUC-101", item1, "seller_vip", 270_000_000.0, "bidder_vip99",
                AuctionStatus.RUNNING, LocalDateTime.now().minusMinutes(45), LocalDateTime.now().plusHours(2), 5_000_000.0);
        auction1.setTotalBids(4);

        Auction auction2 = new Auction("AUC-102", item2, "seller_art", 45_000_000.0, null,
                AuctionStatus.OPEN, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1), 1_000_000.0);

        Auction auction3 = new Auction("AUC-103", item3, "seller_tech", 82_000_000.0, "bidder_hieu",
                AuctionStatus.RUNNING, LocalDateTime.now().minusMinutes(20), LocalDateTime.now().plusMinutes(50), 1_000_000.0);
        auction3.setTotalBids(5);

        Auction auction4 = new Auction("AUC-104", item4, "seller_art", 185_000_000.0, "collector_sg",
                AuctionStatus.FINISHED, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(3), 2_000_000.0);
        auction4.setTotalBids(8);
        Auction auction5 = new Auction("AUC-105", item6, "seller_home", 4_500_000_000.0, null,
                AuctionStatus.OPEN, LocalDateTime.now().plusHours(3), LocalDateTime.now().plusDays(2), 50_000_000.0);
        Auction auction6 = new Auction("AUC-106", item7, "seller_auto", 1_200_000_000.0, null,
                AuctionStatus.OPEN, LocalDateTime.now().plusHours(6), LocalDateTime.now().plusDays(1), 20_000_000.0);

        auctions.add(auction1);
        auctions.add(auction2);
        auctions.add(auction3);
        auctions.add(auction4);
        auctions.add(auction5);
        auctions.add(auction6);

        // Tạo lịch sử bid mẫu cho phiên 101 và 103 (để vẽ LineChart)
        List<Bid> bids101 = new ArrayList<>();
        bids101.add(new Bid("B1", "AUC-101", "seller_vip", 250_000_000.0, LocalDateTime.now().minusMinutes(40)));
        bids101.add(new Bid("B2", "AUC-101", "bidder_john", 255_000_000.0, LocalDateTime.now().minusMinutes(30)));
        bids101.add(new Bid("B3", "AUC-101", "bidder_anna", 262_000_000.0, LocalDateTime.now().minusMinutes(18)));
        bids101.add(new Bid("B4", "AUC-101", "bidder_vip99", 270_000_000.0, LocalDateTime.now().minusMinutes(5)));
        bidHistories.put("AUC-101", bids101);

        List<Bid> bids103 = new ArrayList<>();
        bids103.add(new Bid("B11", "AUC-103", "seller_tech", 75_000_000.0, LocalDateTime.now().minusMinutes(18)));
        bids103.add(new Bid("B12", "AUC-103", "buyer_x", 77_000_000.0, LocalDateTime.now().minusMinutes(14)));
        bids103.add(new Bid("B13", "AUC-103", "buyer_y", 79_000_000.0, LocalDateTime.now().minusMinutes(9)));
        bids103.add(new Bid("B14", "AUC-103", "buyer_x", 80_500_000.0, LocalDateTime.now().minusMinutes(4)));
        bids103.add(new Bid("B15", "AUC-103", "bidder_hieu", 82_000_000.0, LocalDateTime.now().minusMinutes(1)));
        bidHistories.put("AUC-103", bids103);
    }

    @Override
    public boolean login(String username, String password, UserRole role) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return false;
        }
        String account = username.trim().toLowerCase();
        User registeredUser = registeredUsers.values().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(account)
                        || user.getEmail().equalsIgnoreCase(account))
                .findFirst()
                .orElse(null);
        if (registeredUser != null) {
            String registeredPassword = registeredPasswords.get(registeredUser.getUsername().toLowerCase());
            if (registeredPassword == null || !registeredPassword.equals(password)) {
                return false;
            }
            currentUser = registeredUser;
            return true;
        }
        UserRole actualRole;
        String canonicalUsername;
        String fullName;
        String email;
        if ((BIDDER_USERNAME.equals(account) || "hieu@auction.vn".equals(account))
                && DEMO_PASSWORD.equals(password)) {
            actualRole = UserRole.BIDDER;
            canonicalUsername = BIDDER_USERNAME;
            fullName = "Mai Đức Hiệu";
            email = "hieu@auction.vn";
        } else if ((SELLER_USERNAME.equals(account) || "seller@auction.vn".equals(account))
                && DEMO_PASSWORD.equals(password)) {
            actualRole = UserRole.SELLER;
            canonicalUsername = SELLER_USERNAME;
            fullName = "Nguyễn Minh Anh";
            email = "seller@auction.vn";
        } else {
            return false;
        }
        this.currentUser = new User("U-" + canonicalUsername, canonicalUsername,
                fullName, email, actualRole, 100_000_000.0);
        return true;
    }

    @Override
    public boolean register(String username, String password, String fullName, String email, UserRole role) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()
                || fullName == null || fullName.trim().isEmpty() || email == null || email.trim().isEmpty()
                || role == null) {
            return false;
        }
        String canonicalUsername = username.trim();
        String accountKey = canonicalUsername.toLowerCase();
        if (accountKey.equals(BIDDER_USERNAME) || accountKey.equals(SELLER_USERNAME)
                || "hieu@auction.vn".equalsIgnoreCase(email.trim())
                || "seller@auction.vn".equalsIgnoreCase(email.trim())
                || registeredUsers.containsKey(accountKey)
                || registeredUsers.values().stream().anyMatch(user -> user.getEmail().equalsIgnoreCase(email.trim()))) {
            return false;
        }
        User user = new User("U-" + System.currentTimeMillis() % 1000, canonicalUsername,
                fullName.trim(), email.trim(), role, 50_000_000.0);
        registeredUsers.put(accountKey, user);
        registeredPasswords.put(accountKey, password);
        this.currentUser = user;
        return true;
    }

    @Override
    public User getCurrentUser() {
        return currentUser;
    }

    @Override
    public void logout() {
        this.currentUser = null;
    }

    @Override
    public List<Auction> getAuctions(AuctionStatus filterStatus) {
        refreshAuctionStatuses();
        if (filterStatus == null) {
            return new ArrayList<>(auctions);
        }
        return auctions.stream()
                .filter(a -> a.getStatus() == filterStatus)
                .collect(Collectors.toList());
    }

    @Override
    public Auction getAuctionById(String auctionId) {
        return auctions.stream()
                .filter(a -> a.getId().equalsIgnoreCase(auctionId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public synchronized BidResult placeBid(String auctionId, double amount) {
        Auction auction = getAuctionById(auctionId);
        if (auction == null) {
            return BidResult.fail("Không tìm thấy phiên đấu giá.");
        }

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            return BidResult.fail("Phiên đấu giá hiện không ở trạng thái đang diễn ra (" + auction.getStatus().getDescription() + ").");
        }
        if (auction.getEndTime() == null || !auction.getEndTime().isAfter(LocalDateTime.now())) {
            auction.setStatus(AuctionStatus.FINISHED);
            return BidResult.fail("Phiên đấu giá đã kết thúc. Không thể nhận thêm lượt đặt giá.");
        }
        if (!Double.isFinite(amount) || amount <= 0 || amount != Math.rint(amount)) {
            return BidResult.fail("Giá đặt phải là số nguyên dương.");
        }

        double minimumAcceptable = auction.getCurrentPrice() + auction.getMinBidStep();
        if (amount < minimumAcceptable) {
            return BidResult.fail("Giá đặt (" + com.auction.client.util.FormatUtil.formatCurrency(amount) +
                    ") không hợp lệ. Phải lớn hơn hoặc bằng mức giá tối thiểu (" +
                    com.auction.client.util.FormatUtil.formatCurrency(minimumAcceptable) + ").");
        }

        String username = currentUser != null ? currentUser.getUsername() : "Anonymous";

        // Cập nhật giá và người đấu cao nhất
        auction.setCurrentPrice(amount);
        auction.setHighestBidderUsername(username);
        auction.setTotalBids(auction.getTotalBids() + 1);

        Bid newBid = new Bid("BID-" + System.currentTimeMillis(), auctionId, username, amount, LocalDateTime.now());
        bidHistories.computeIfAbsent(auctionId, k -> new ArrayList<>()).add(newBid);

        // Kích hoạt Observer thông báo tới các listeners (để cập nhật biểu đồ / UI)
        notifyListeners(auctionId, newBid);

        return BidResult.ok("Đặt giá thành công!", amount, newBid);
    }

    @Override
    public List<Bid> getBidHistory(String auctionId) {
        List<Bid> history = bidHistories.get(auctionId);
        if (history == null) return Collections.emptyList();
        return new ArrayList<>(history);
    }

    @Override
    public List<Item> getSellerItems(String sellerUsername) {
        return items.stream()
                .filter(i -> sellerUsername == null || sellerUsername.equalsIgnoreCase(i.getSellerUsername()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean createItemAndAuction(Item item, int durationMinutes, double minBidStep) {
        if (item == null) return false;
        items.add(item);

        Auction newAuction = new Auction(
                "AUC-" + (100 + auctions.size() + 1),
                item,
                item.getSellerUsername(),
                item.getStartingPrice(),
                null,
                AuctionStatus.RUNNING,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(durationMinutes),
                minBidStep > 0 ? minBidStep : 500_000.0
        );
        auctions.add(newAuction);
        return true;
    }

    @Override
    public boolean updateItem(Item item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(item.getId())) {
                items.set(i, item);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deleteItem(String itemId) {
        refreshAuctionStatuses();
        Auction activeAuction = auctions.stream()
                .filter(a -> a.getItem() != null && a.getItem().getId().equals(itemId))
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                .findFirst()
                .orElse(null);
        if (activeAuction != null) {
            return false;
        }
        boolean removed = items.removeIf(i -> i.getId().equals(itemId));
        auctions.removeIf(a -> a.getItem() != null && a.getItem().getId().equals(itemId));
        return removed;
    }

    private void refreshAuctionStatuses() {
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : auctions) {
            if (auction.getEndTime() != null
                    && !now.isBefore(auction.getEndTime())) {
                auction.setStatus(AuctionStatus.FINISHED);
            } else if (auction.getStatus() == AuctionStatus.OPEN
                    && auction.getStartTime() != null
                    && !now.isBefore(auction.getStartTime())) {
                auction.setStatus(AuctionStatus.RUNNING);
            }
        }
    }

    @Override
    public void registerBidListener(String auctionId, Consumer<Bid> listener) {
        listeners.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unregisterBidListener(String auctionId, Consumer<Bid> listener) {
        List<Consumer<Bid>> list = listeners.get(auctionId);
        if (list != null) {
            list.remove(listener);
        }
    }

    private void notifyListeners(String auctionId, Bid bid) {
        List<Consumer<Bid>> list = listeners.get(auctionId);
        if (list != null) {
            for (Consumer<Bid> listener : list) {
                try {
                    listener.accept(bid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
