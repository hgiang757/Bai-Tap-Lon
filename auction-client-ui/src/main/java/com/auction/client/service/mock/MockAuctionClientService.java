package com.auction.client.service.mock;

import com.auction.client.service.AuctionClientService;
import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.Bid;
import com.auction.client.model.Item;
import com.auction.client.model.User;
import com.auction.client.model.UserRole;
import com.auction.client.service.BidResult;
import com.auction.client.service.ServiceRegistry;

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
    private static final String ADMIN_USERNAME = "admin_hieu";
    private User currentUser;
    private final MockDataStore store;
    private final List<Auction> auctions;
    private final List<Item> items;
    private final Map<String, String> registeredPasswords = new ConcurrentHashMap<>();
    private final Map<String, User> registeredUsers = new ConcurrentHashMap<>();
    private final Map<String, List<Bid>> bidHistories;
    private final Map<String, List<Consumer<Bid>>> listeners = new ConcurrentHashMap<>();

    public MockAuctionClientService() { this(MockDataStore.getInstance()); }

    /** Khởi tạo dịch vụ với kho được chỉ định. */
    public MockAuctionClientService(MockDataStore store) {
        this.store = store;
        this.auctions = store.auctions;
        this.items = store.items;
        this.bidHistories = store.bidHistories;
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
            store.attachUser(currentUser);
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
        } else if (ADMIN_USERNAME.equals(account) && DEMO_PASSWORD.equals(password)) {
            actualRole = UserRole.ADMIN;
            canonicalUsername = ADMIN_USERNAME;
            fullName = "Quản trị viên";
            email = "admin@auction.vn";
        } else {
            return false;
        }
        // Tài khoản mẫu dùng chung trạng thái và vai trò với màn hình quản trị mock.
        final String demoUsername = canonicalUsername;
        com.auction.client.model.AdminUser managed = ServiceRegistry.getUserAdminService()
                .listUsers().stream().filter(u -> u.getUser().getUsername().equals(demoUsername))
                .findFirst().orElse(null);
        if (managed == null || managed.isLocked()) {
            return false;
        }
        actualRole = managed.getUser().getRole();
        this.currentUser = new User("U-" + canonicalUsername, canonicalUsername,
                fullName, email, actualRole, 100_000_000.0);
        store.attachUser(currentUser);
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
                || accountKey.equals(ADMIN_USERNAME)
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
        store.attachUser(user);
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
    public List<Auction> getAuctions(AuctionStatus filterStatus) { synchronized (store) {
        refreshAuctionStatuses();
        if (filterStatus == null) {
            return new ArrayList<>(auctions);
        }
        return auctions.stream()
                .filter(a -> a.getStatus() == filterStatus)
                .collect(Collectors.toList());
     }}

    @Override
    public Auction getAuctionById(String auctionId) { synchronized (store) {
        refreshAuctionStatuses();
        return auctions.stream()
                .filter(a -> a.getId().equalsIgnoreCase(auctionId))
                .findFirst()
                .orElse(null);
     }}

    @Override
    public BidResult placeBid(String auctionId, double amount) { synchronized (store) {
        if (currentUser == null) return BidResult.fail("Vui lòng đăng nhập để đặt giá.");
        Auction auction = getAuctionById(auctionId);
        if (auction == null) {
            return BidResult.fail("Không tìm thấy phiên đấu giá.");
        }

        if (currentUser.getUsername().equalsIgnoreCase(auction.getSellerUsername())) {
            return BidResult.fail("Bạn không thể đặt giá cho sản phẩm của mình.");
        }

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            return BidResult.fail("Phiên đấu giá hiện không ở trạng thái đang diễn ra (" + auction.getStatus().getDescription() + ").");
        }
        if (auction.getEndTime() == null || !auction.getEndTime().isAfter(LocalDateTime.now())) {
            refreshAuctionStatuses();
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

        String username = currentUser.getUsername();

        if (amount > Long.MAX_VALUE || !store.holdBid(auctionId, username, (long) amount)) {
            return BidResult.fail("Số dư khả dụng không đủ. Hãy nạp thêm tiền trong mục Ví.");
        }

        // Cập nhật giá và người đấu cao nhất
        auction.setCurrentPrice(amount);
        auction.setHighestBidderUsername(username);
        auction.setTotalBids(auction.getTotalBids() + 1);

        Bid newBid = new Bid("BID-" + System.currentTimeMillis(), auctionId, username, amount, LocalDateTime.now());
        bidHistories.computeIfAbsent(auctionId, k -> new ArrayList<>()).add(newBid);

        // Kích hoạt Observer thông báo tới các listeners (để cập nhật biểu đồ / UI)
        notifyListeners(auctionId, newBid);

        return BidResult.ok("Đặt giá thành công!", amount, newBid);
     }}

    @Override
    public List<Bid> getBidHistory(String auctionId) { synchronized (store) {
        List<Bid> history = bidHistories.get(auctionId);
        if (history == null) return Collections.emptyList();
        return new ArrayList<>(history);
     }}

    @Override
    public List<Item> getSellerItems(String sellerUsername) { synchronized (store) {
        return items.stream()
                .filter(i -> sellerUsername == null || sellerUsername.equalsIgnoreCase(i.getSellerUsername()))
                .collect(Collectors.toList());
     }}

    @Override
    public boolean createItemAndAuction(Item item, int durationMinutes, double minBidStep) { synchronized (store) {
        if (item == null) return false;
        items.add(item);

        Auction newAuction = new Auction(
                "AUC-" + java.util.UUID.randomUUID(),
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
     }}

    @Override
    public boolean updateItem(Item item) { synchronized (store) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(item.getId())) {
                items.set(i, item);
                auctions.stream().filter(a -> a.getItem() != null && a.getItem().getId().equals(item.getId()))
                        .forEach(a -> a.setItem(item));
                return true;
            }
        }
        return false;
     }}

    @Override
    public boolean deleteItem(String itemId) { synchronized (store) {
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
     }}

    private void refreshAuctionStatuses() { store.refreshStatuses(); }

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
