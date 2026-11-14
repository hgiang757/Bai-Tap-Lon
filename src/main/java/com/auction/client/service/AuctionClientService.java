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
 * Service Interface định nghĩa các phương thức giao tiếp giữa Client và Server. Giúp Controller
 * hoàn toàn tách biệt khỏi logic tầng mạng (Network/Socket).
 */
public interface AuctionClientService {

  // === Xác thực & Quản lý phiên ===
  /** Authenticates the account; the server determines its actual role. */
  boolean login(String username, String password, UserRole role);

  /** Registers an account using the supplied profile and participant role. */
  boolean register(String username, String password, String fullName, String email, UserRole role);

  /** Returns the current client account, or null when signed out. */
  User getCurrentUser();

  /** Ends the current client session. */
  void logout();

  // === Đấu giá & Danh sách phiên ===
  /** Returns auctions matching the optional status filter. */
  List<Auction> getAuctions(AuctionStatus filterStatus);

  /** Returns the latest snapshot of the requested auction. */
  Auction getAuctionById(String auctionId);

  /** Submits a bid and reports acceptance or a known rejection. */
  BidResult placeBid(String auctionId, double amount);

  /** Returns the accepted bids for an auction. */
  List<Bid> getBidHistory(String auctionId);

  // === Quản lý sản phẩm (Dành cho Seller) ===
  /** Returns products associated with the specified seller. */
  List<Item> getSellerItems(String sellerUsername);

  /** Creates a product and its auction using the supplied duration and step. */
  boolean createItemAndAuction(Item item, int durationMinutes, double minBidStep);

  /** Updates a product through the product service. */
  boolean updateItem(Item item);

  /** Deletes an editable product through the product service. */
  boolean deleteItem(String itemId);

  // === Observer / Realtime Listener Pattern ===
  /** Registers a callback for accepted bids in one auction. */
  void registerBidListener(String auctionId, Consumer<Bid> listener);

  /** Removes a previously registered auction bid callback. */
  void unregisterBidListener(String auctionId, Consumer<Bid> listener);

  /** General state/connection events; callbacks may run on a background thread. */
  default void addChangeListener(Consumer<String> listener) {}

  /** Removes a callback for general state and connection changes. */
  default void removeChangeListener(Consumer<String> listener) {}

  /** Resolve durable pending commands before accepting another price operation. */
  default boolean recoverPendingCommands() {
    return false;
  }

  /** Returns the service clock used to display and validate auction times. */
  default java.time.LocalDateTime serverNow() {
    return java.time.LocalDateTime.now();
  }

  /** Reads an auction together with its accepted bid history. */
  default AuctionRoomSnapshot getRoomSnapshot(String id) {
    return new AuctionRoomSnapshot(getAuctionById(id), getBidHistory(id));
  }
}
