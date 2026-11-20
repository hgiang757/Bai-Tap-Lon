package com.auction.server.dao;

import static com.auction.shared.JsonProtocol.number;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;

/** Ánh xạ đối tượng nghiệp vụ theo schemaVersion=1 và bảo toàn các trường JSON khác. */
public final class AuctionJsonMapper {
  private AuctionJsonMapper() {}

  /** Tạo đối tượng phiên đấu giá bất biến từ dữ liệu JSON đã lưu. */
  public static Auction auction(JsonObject json) {
    return new Auction(
        text(json, "id"),
        text(json, "sellerId"),
        number(json, "currentPrice"),
        number(json, "minBidStep"),
        AuctionStatus.valueOf(text(json, "status")),
        LocalDateTime.parse(text(json, "startTime")),
        LocalDateTime.parse(text(json, "endTime")),
        text(json, "highestBidderId"),
        number(json, "totalBids"),
        json.get("settled").getAsBoolean());
  }

  /** Cập nhật các trường đặt giá vào JSON hiện tại và bảo toàn các trường mở rộng. */
  public static void applyBid(JsonObject target, Auction accepted, String username) {
    target.addProperty("highestBidderId", accepted.highestBidderId());
    target.addProperty("highestBidderUsername", username);
    target.addProperty("currentPrice", accepted.currentPrice());
    target.addProperty("totalBids", accepted.totalBids());
  }

  /** Ghi lượt đặt giá đã chấp nhận bằng tên trường JSON hiện có. */
  public static JsonObject bid(BidTransaction bid) {
    return obj(
        "id", bid.id(),
        "auctionId", bid.auctionId(),
        "bidderUsername", bid.bidderUsername(),
        "amount", bid.amount(),
        "timestamp", bid.timestamp(),
        "auto", bid.automatic());
  }
}
