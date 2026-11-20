package com.auction.server.model;

import com.auction.server.exception.ApiException;
import com.auction.server.exception.AuctionClosedException;
import com.auction.server.exception.InvalidBidException;
import java.time.LocalDateTime;

/**
 * Luật đấu giá cho một bản chụp trạng thái bất biến. Lớp dịch vụ quản lý ví, lưu dữ liệu
 * và giữ khóa, đồng thời dựng lại đối tượng trong mỗi giao dịch để tránh dùng trạng thái cũ.
 */
public record Auction(
    String id,
    String sellerId,
    long currentPrice,
    long minBidStep,
    AuctionStatus status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String highestBidderId,
    long totalBids,
    boolean settled) {

  /** Tạo bản chụp của phiên chưa có lượt đặt giá. */
  public Auction(
      String id,
      String sellerId,
      long currentPrice,
      long minBidStep,
      String status,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    this(
        id,
        sellerId,
        currentPrice,
        minBidStep,
        AuctionStatus.valueOf(status),
        startTime,
        endTime,
        "",
        0,
        !AuctionStatus.valueOf(status).isActive());
  }

  /** Trả về giá hiện tại cộng với bước giá tối thiểu. */
  public long minimumNextBid() {
    return currentPrice + minBidStep;
  }

  /** Kiểm tra quyền đặt giá theo đồng hồ máy chủ và tài khoản đã xác thực. */
  public void requireBidder(String userId, String role, LocalDateTime time) {
    if ("ADMIN".equals(role) || sellerId.equals(userId)) {
      throw new ApiException("FORBIDDEN", "Cannot bid on this product");
    }
    if (startTime.isAfter(time)) {
      throw new ApiException("AUCTION_NOT_STARTED", "Auction has not started");
    }
    if (status != AuctionStatus.RUNNING || !time.isBefore(endTime)) {
      throw new AuctionClosedException("Auction is closed");
    }
  }

  /** Lớp dịch vụ kiểm tra định dạng, giới hạn giá và số dư khả dụng. */
  public void requireMinimumBid(long amount) {
    if (amount < minimumNextBid()) {
      throw new InvalidBidException("Bid must reach current price plus minimum step");
    }
  }

  /** Từ chối giá trần hoặc bước giá thấp hơn mức tối thiểu của phiên. */
  public void requireAutoBid(long maximum, long increment) {
    if (increment < minBidStep || maximum < minimumNextBid()) {
      throw new ApiException("VALIDATION_ERROR", "Invalid auto-bid step or maximum");
    }
  }

  /** Chỉ cho sửa sản phẩm của phiên sắp mở và chưa có lượt đặt giá. */
  public void requireEditable() {
    if (status != AuctionStatus.OPEN || totalBids != 0) {
      throw new ApiException("CONFLICT", "Only upcoming auctions without bids are editable");
    }
  }

  /** Chỉ cho hủy phiên khi phiên còn hiệu lực. */
  public void requireCancelable() {
    if (!status.isActive()) {
      throw new ApiException("CONFLICT", "Auction cannot be canceled");
    }
  }

  /** Kiểm tra tài khoản có đang dẫn đầu phiên hay không. */
  public boolean isLeading(String userId) {
    return !highestBidderId.isEmpty() && highestBidderId.equals(userId);
  }

  /** Trả về số tiền đang giữ của tài khoản khi phiên chưa quyết toán. */
  public long heldBy(String userId) {
    return !settled && isLeading(userId) ? currentPrice : 0;
  }

  /** Kiểm tra phiên đang hoạt động đã tới hạn kết thúc hay chưa. */
  public boolean hasExpired(LocalDateTime time) {
    return status.isActive() && !time.isBefore(endTime);
  }

  /** Kiểm tra phiên sắp mở đã tới giờ bắt đầu hay chưa. */
  public boolean shouldStart(LocalDateTime time) {
    return status == AuctionStatus.OPEN && !time.isBefore(startTime);
  }

  /** Tạo bản chụp mới với người dẫn đầu là tài khoản vừa đặt giá hợp lệ. */
  public Auction withBid(String bidderId, long amount) {
    requireMinimumBid(amount);
    return new Auction(
        id,
        sellerId,
        amount,
        minBidStep,
        status,
        startTime,
        endTime,
        bidderId,
        totalBids + 1,
        settled);
  }
}
