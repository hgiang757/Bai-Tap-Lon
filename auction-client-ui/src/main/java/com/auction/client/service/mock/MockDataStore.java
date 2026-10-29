package com.auction.client.service.mock;

import com.auction.client.model.*;
import java.time.LocalDateTime;
import java.util.*;

/** Kho dữ liệu giả dùng chung trong bộ nhớ; mọi truy cập đồng bộ trên kho. */
public final class MockDataStore {
    private static final MockDataStore INSTANCE = new MockDataStore();
    final List<Auction> auctions = new ArrayList<>();
    final List<Item> items = new ArrayList<>();
    final List<AdminUser> users = new ArrayList<>();
    final Map<String, String> images = new HashMap<>();
    final Map<String, String> cancellationReasons = new HashMap<>();
    final Map<String, List<Bid>> bidHistories = new HashMap<>();

    final Map<String, Long> balances = new HashMap<>();
    final Map<String, User> walletUsers = new HashMap<>();
    final Map<String, Hold> holds = new HashMap<>();
    final Map<String, List<Transaction>> walletHistory = new HashMap<>();
    record Hold(String username, long amount) { }

    void attachUser(User user) {
        synchronized (this) {
            String name = user.getUsername();
            balances.putIfAbsent(name, (long) user.getBalance());
            walletUsers.put(name, user);
            user.setBalance(balances.get(name));
        }
    }
    long balance(String user) { return balances.getOrDefault(user, 0L); }
    long held(String user) { return holds.values().stream().filter(h -> h.username().equals(user)).mapToLong(Hold::amount).sum(); }
    List<Transaction> transactions(String user) {
        List<Transaction> result = new ArrayList<>(walletHistory.getOrDefault(user, List.of()));
        Collections.reverse(result);
        return result;
    }
    void log(String user, TransactionType type, long amount, String auction) {
        walletHistory.computeIfAbsent(user, key -> new ArrayList<>()).add(new Transaction(
                UUID.randomUUID().toString(), user, type, amount, balance(user), LocalDateTime.now(), "Thành công", "", auction));
    }
    void changeBalance(String user, long value, TransactionType type, long amount, String auction) {
        balances.put(user, value);
        User account = walletUsers.get(user);
        if (account != null) account.setBalance(value);
        log(user, type, amount, auction);
    }
    boolean holdBid(String auctionId, String user, long amount) {
        Hold previous = holds.get(auctionId);
        long existing = previous != null && previous.username().equals(user) ? previous.amount() : 0;
        if (balance(user) - held(user) + existing < amount) return false;
        if (previous != null) {
            holds.remove(auctionId);
            log(previous.username(), TransactionType.BID_RELEASE, previous.amount(), auctionId);
        }
        holds.put(auctionId, new Hold(user, amount));
        log(user, TransactionType.BID_HOLD, amount, auctionId);
        return true;
    }
    void releaseHold(String auctionId) {
        Hold hold = holds.remove(auctionId);
        if (hold != null) log(hold.username(), TransactionType.BID_RELEASE, hold.amount(), auctionId);
    }
    private void settle(Auction auction) {
        Hold hold = holds.remove(auction.getId());
        if (hold == null) return;
        if (hold.username().equals(auction.getHighestBidderUsername())) {
            changeBalance(hold.username(), balance(hold.username()) - hold.amount(), TransactionType.PAYMENT, hold.amount(), auction.getId());
            auction.setStatus(AuctionStatus.PAID);
        } else log(hold.username(), TransactionType.BID_RELEASE, hold.amount(), auction.getId());
    }

    private MockDataStore() {
        initSampleData(); initManagementData();
        balances.put("bidder_hieu", 100_000_000L);
        balances.put("bidder_vip99", 300_000_000L);
        for (Auction auction : auctions) {
            if (auction.getStatus() == AuctionStatus.RUNNING && auction.getTotalBids() > 0) {
                String winner = auction.getHighestBidderUsername();
                long amount = (long) auction.getCurrentPrice();
                if (balance(winner) - held(winner) >= amount) holdBid(auction.getId(), winner, amount);
            }
        }
    }
    /** Lấy kho dùng chung của ứng dụng. */
    public static MockDataStore getInstance() { return INSTANCE; }
    /** Tạo kho độc lập phục vụ kiểm thử. */
    public static MockDataStore createForTesting() { return new MockDataStore(); }
    /** Cập nhật trạng thái các phiên theo thời gian. */
    public synchronized void refreshStatuses() {
        LocalDateTime now = LocalDateTime.now();
        for (Auction a : auctions) {
            if (a.getStatus() == AuctionStatus.FINISHED) { settle(a); continue; }
            if (a.getStatus() == AuctionStatus.CANCELED) { releaseHold(a.getId()); continue; }
            if (a.getStatus() != AuctionStatus.OPEN && a.getStatus() != AuctionStatus.RUNNING) continue;
            if (a.getEndTime() != null && !now.isBefore(a.getEndTime())) { a.setStatus(AuctionStatus.FINISHED); settle(a); }
            else if (a.getStatus() == AuctionStatus.OPEN && a.getStartTime() != null && !now.isBefore(a.getStartTime())) a.setStatus(AuctionStatus.RUNNING);
        }
    }
    private void initSampleData() {
        // Thiết lập người dùng mặc định


        // Tạo danh mục sản phẩm mẫu
        Item item1 = new Item("IT01", "Đồng hồ Rolex Submariner Date", "Tình trạng như mới 99%, đầy đủ hộp sổ thẻ chính hãng.", "Đồng hồ cao cấp", 250_000_000.0, "seller_vip");
        Item item2 = new Item("IT02", "Tranh sơn mài Cổ Đồ", "Tác phẩm thủ công độc bản từ nghệ nhân Hà Nội thế kỷ 20.", "Nghệ thuật & Cổ vật", 45_000_000.0, "seller_art");
        Item item3 = new Item("IT03", "MacBook Pro M4 Max 64GB 1TB", "Máy mới nguyên seal, bảo hành Apple Care+ 3 năm.", "Thiết bị công nghệ", 75_000_000.0, "seller_tech");
        Item item4 = new Item("IT04", "Tượng Phật đồng thời Lý", "Hiện vật cổ có giấy chứng nhận thẩm định bảo tàng.", "Cổ vật quý", 120_000_000.0, "seller_art");
        Item item5 = new Item("IT05", "Vòng cổ ngọc trai thiên nhiên", "Ngọc trai tuyển chọn, kèm giấy kiểm định chất lượng.", "Trang sức & Đá quý", 35_000_000.0, "seller_hieu");
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

    private void initManagementData() {
        LocalDateTime now = LocalDateTime.now();
        users.add(user("admin_hieu", "admin@auction.vn", UserRole.ADMIN, now.minusDays(60)));
        users.add(user("seller_hieu", "seller@auction.vn", UserRole.SELLER, now.minusDays(40)));
        users.add(user("bidder_hieu", "hieu@auction.vn", UserRole.BIDDER, now.minusDays(30)));
        add("M01", "Tranh sơn mài", "Art", "seller_hieu", AuctionStatus.OPEN, now.plusDays(1), now.plusDays(2), 0);
        add("M02", "Máy ảnh cổ", "Electronics", "seller_hieu", AuctionStatus.RUNNING, now.minusHours(1), now.plusHours(3), 2);
        add("M03", "Xe cổ", "Vehicle", "seller_auto", AuctionStatus.FINISHED, now.minusDays(2), now.minusDays(1), 3);
        add("M04", "Tác phẩm nghệ thuật", "Art", "seller_art", AuctionStatus.CANCELED, now.plusDays(2), now.plusDays(3), 0);
        add("M05", "Máy tính xách tay", "Electronics", "seller_tech", AuctionStatus.PAID, now.minusDays(3), now.minusDays(2), 4);
    }
    private AdminUser user(String name, String email, UserRole role, LocalDateTime date) {
        return new AdminUser(new User("U-" + name, name, name, email, role, 0), date);
    }
    private void add(String id, String name, String type, String seller, AuctionStatus status,
                     LocalDateTime start, LocalDateTime end, int bids) {
        Item item = new Item("I-" + id, name, "Sản phẩm minh họa", type, 1000000, seller);
        Auction auction = new Auction(id, item, seller, 1000000 + bids * 100000,
                bids > 0 ? "bidder_hieu" : null, status, start, end, 100000);
        auction.setTotalBids(bids);
        auctions.add(auction);
        items.add(item);
        List<Bid> history = new ArrayList<>();
        for (int i = 1; i <= bids; i++) history.add(new Bid(id + "-" + i, id, "bidder_hieu", item.getStartingPrice() + i * auction.getMinBidStep(), start.plusMinutes(i)));
        bidHistories.put(id, history);
    }
}
