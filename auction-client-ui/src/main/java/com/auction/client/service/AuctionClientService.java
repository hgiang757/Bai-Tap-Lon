package com.auction.client.service;

import com.auction.client.model.Auction;
import com.auction.client.model.AuctionStatus;
import com.auction.client.model.Bid;
import com.auction.client.model.Item;
import com.auction.client.model.User;
import com.auction.client.model.UserRole;

import java.util.List;
import java.util.function.Consumer;

/**
 * Service Interface định nghĩa các phương thức giao tiếp giữa Client và Server.
 * Giúp Controller hoàn toàn tách biệt khỏi logic tầng mạng (Network/Socket).
 */
public interface AuctionClientService {

    // === Xác thực & Quản lý phiên ===
    boolean login(String username, String password, UserRole role);
    boolean register(String username, String password, String fullName, String email, UserRole role);
    User getCurrentUser();
    void logout();

    // === Đấu giá & Danh sách phiên ===
    List<Auction> getAuctions(AuctionStatus filterStatus);
    Auction getAuctionById(String auctionId);
    BidResult placeBid(String auctionId, double amount);
    List<Bid> getBidHistory(String auctionId);

    // === Quản lý sản phẩm (Dành cho Seller) ===
    List<Item> getSellerItems(String sellerUsername);
    boolean createItemAndAuction(Item item, int durationMinutes, double minBidStep);
    boolean updateItem(Item item);
    boolean deleteItem(String itemId);

    // === Observer / Realtime Listener Pattern ===
    void registerBidListener(String auctionId, Consumer<Bid> listener);
    void unregisterBidListener(String auctionId, Consumer<Bid> listener);
}
