package com.hieu.services;

import com.hieu.models.Auction;
import com.hieu.models.AuctionStatus;
import com.hieu.models.Bid;
import com.hieu.models.Item;
import com.hieu.models.User;
import com.hieu.models.UserRole;

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

    private User currentUser;
    private final List<Auction> auctions = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
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

        items.add(item1);
        items.add(item2);
        items.add(item3);
        items.add(item4);

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

        auctions.add(auction1);
        auctions.add(auction2);
        auctions.add(auction3);
        auctions.add(auction4);

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
        UserRole actualRole = role != null ? role :
                (username.trim().toLowerCase().startsWith("seller") ? UserRole.SELLER : UserRole.BIDDER);
        this.currentUser = new User("U-" + System.currentTimeMillis() % 1000, username.trim(),
                username.trim(), username.trim() + "@auction.vn", actualRole, 100_000_000.0);
        return true;
    }

    @Override
    public boolean register(String username, String password, String fullName, String email, UserRole role) {
        if (username == null || username.trim().isEmpty()) return false;
        this.currentUser = new User("U-" + System.currentTimeMillis() % 1000, username.trim(),
                fullName, email, role, 50_000_000.0);
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

        double minimumAcceptable = auction.getCurrentPrice() + auction.getMinBidStep();
        if (amount < minimumAcceptable) {
            return BidResult.fail("Giá đặt (" + com.hieu.util.FormatUtil.formatCurrency(amount) +
                    ") không hợp lệ. Phải lớn hơn hoặc bằng mức giá tối thiểu (" +
                    com.hieu.util.FormatUtil.formatCurrency(minimumAcceptable) + ").");
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
        items.removeIf(i -> i.getId().equals(itemId));
        auctions.removeIf(a -> a.getItem() != null && a.getItem().getId().equals(itemId));
        return true;
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
