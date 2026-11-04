package com.auction.server.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    private String auctionId;
    private Item item;            // Quan hệ Aggregation: Phiên này bán sản phẩm nào
    private double startingPrice;
    private double currentPrice;
    private double stepPrice;     // Bước giá

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;        // OPEN, RUNNING, FINISHED

    // Quan hệ Composition (Hoặc Aggregation): Lưu trữ lịch sử các lượt đặt giá
    private List<Bid> bidHistory;

    // Người đang giữ giá cao nhất
    private Bidder currentHighestBidder;

    public Auction(String auctionId, Item item, double startingPrice, double stepPrice, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.item = item;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice; // Lúc mới mở, giá hiện tại bằng giá khởi điểm
        this.stepPrice = stepPrice;
        this.startTime = LocalDateTime.now();
        this.endTime = endTime;
        this.status = "OPEN";
        this.bidHistory = new ArrayList<>(); // Tự khởi tạo mảng quản lý lịch sử (Composition)
    }

    // Các Getter và Setter cơ bản (Bạn có thể tự Generate bằng Alt+Insert trong IntelliJ)
    // ...

    public double getCurrentPrice() {
        return currentPrice;
    }

    public double getStepPrice() {
        return stepPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public Item getItem() {
        return item;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    // Dùng cho anti-sniping (gia hạn phiên)
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Bidder getCurrentHighestBidder() {
        return currentHighestBidder;
    }

    // Trả về bản chỉ đọc để bên ngoài không sửa trực tiếp lịch sử
    public List<Bid> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    // Hành động thêm lượt đặt giá mới vào lịch sử
    public void addBid(Bid bid) {
        this.bidHistory.add(bid);
        this.currentPrice = bid.getBidAmount();
        this.currentHighestBidder = bid.getBidder();
    }
}