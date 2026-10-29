package com.auction.client.service.mock;

import com.auction.client.model.AdminUser;
import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.Bid;
import com.auction.client.model.Item;
import com.auction.client.model.User;
import com.auction.client.model.UserRole;
import com.auction.client.service.AuctionAdminService;
import com.auction.client.service.ProductService;
import com.auction.client.service.UserAdminService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Dữ liệu quản trị giả dùng chung giữa các màn hình, chỉ lưu trong bộ nhớ. */
public class MockManagementService implements ProductService, AuctionAdminService, UserAdminService {
    private final MockDataStore store;
    private final List<Auction> auctions;
    private final List<AdminUser> users;
    private final Map<String, String> images;
    private final Map<String, String> cancellationReasons;

    /** Khởi tạo dịch vụ với kho dùng chung. */
    public MockManagementService() { this(MockDataStore.getInstance()); }
    /** Khởi tạo dịch vụ với kho được chỉ định. */
    public MockManagementService(MockDataStore store) {
        this.store = store;
        this.auctions = store.auctions;
        this.users = store.users;
        this.images = store.images;
        this.cancellationReasons = store.cancellationReasons;
    }
    private Auction findAuction(String id) {
        return auctions.stream().filter(a -> a.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy phiên."));
    }
    private AdminUser findUser(String id) {
        return users.stream().filter(u -> u.getUser().getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng."));
    }
    private void checkOther(String actor, String id) {
        if (Objects.equals(actor, id)) throw new IllegalStateException("Không thể thao tác với tài khoản của chính mình.");
    }
    private boolean editable(Auction auction) {
        return auction.getStatus() == AuctionStatus.OPEN && auction.getTotalBids() == 0;
    }
    @Override public List<Auction> listProducts(String seller) { synchronized (store) {
        store.refreshStatuses();
        return auctions.stream().filter(a -> Objects.equals(a.getSellerUsername(), seller)).collect(Collectors.toList());
     }}
    @Override public void saveProduct(Auction auction, String path) { synchronized (store) {
        store.refreshStatuses();
        Auction old = auctions.stream().filter(a -> a.getId().equals(auction.getId())).findFirst().orElse(null);
        if (old != null && (!editable(old) || !Objects.equals(old.getSellerUsername(), auction.getSellerUsername()))) {
            throw new IllegalStateException("Không thể sửa phiên này.");
        }
        if (old != null) { auctions.remove(old); store.items.remove(old.getItem()); }
        auctions.add(auction);
        store.items.add(auction.getItem());
        if (path != null) images.put(auction.getId(), path);
     }}
    @Override public void deleteProduct(String seller, String id) { synchronized (store) {
        store.refreshStatuses();
        Auction auction = findAuction(id);
        if (!Objects.equals(seller, auction.getSellerUsername()) || !editable(auction)) {
            throw new IllegalStateException("Không thể xóa phiên này.");
        }
        auctions.remove(auction);
        store.items.remove(auction.getItem());
        store.bidHistories.remove(id);
        images.remove(id);
     }}
    @Override public String getImagePath(String id) { synchronized (store) { return images.get(id);  }}
    @Override public List<Bid> getBidHistory(String id) { synchronized (store) {
        findAuction(id);
        return new ArrayList<>(store.bidHistories.getOrDefault(id, List.of()));
     }}
    @Override public List<Auction> listAuctions() { synchronized (store) { store.refreshStatuses(); return new ArrayList<>(auctions);  }}
    @Override public void cancelAuction(String id, String reason) { synchronized (store) {
        store.refreshStatuses();
        Auction auction = findAuction(id);
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Cần nhập lý do hủy.");
        if (auction.getStatus() != AuctionStatus.OPEN && auction.getStatus() != AuctionStatus.RUNNING) {
            throw new IllegalStateException("Không thể hủy phiên ở trạng thái này.");
        }
        auction.setStatus(AuctionStatus.CANCELED);
        store.stopAutoBids(id);
        store.releaseHold(id);
        cancellationReasons.put(id, reason);
     }}
    @Override public void deleteAuction(String id) { synchronized (store) {
        store.refreshStatuses();
        Auction auction = findAuction(id);
        if (auction.getStatus() != AuctionStatus.CANCELED) throw new IllegalStateException("Chỉ xóa phiên đã hủy.");
        auctions.remove(auction);
        store.items.remove(auction.getItem());
        store.bidHistories.remove(id);
        images.remove(id);
     }}
    @Override public List<AdminUser> listUsers() { synchronized (store) { return new ArrayList<>(users);  }}
    @Override public void setLocked(String actor, String id, boolean locked) { synchronized (store) {
        checkOther(actor, id); findUser(id).setLocked(locked);
     }}
    @Override public void setRole(String actor, String id, UserRole role) { synchronized (store) {
        checkOther(actor, id); findUser(id).getUser().setRole(Objects.requireNonNull(role));
     }}
    @Override public void deleteUser(String actor, String id) { synchronized (store) {
        checkOther(actor, id); users.remove(findUser(id));
     }}
}
