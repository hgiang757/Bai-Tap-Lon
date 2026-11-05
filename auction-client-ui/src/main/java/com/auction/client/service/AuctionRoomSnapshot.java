package com.auction.client.service;
import com.auction.client.model.Auction;
import com.auction.client.model.Bid;
import java.util.List;
public record AuctionRoomSnapshot(Auction auction, List<Bid> history) {
    public AuctionRoomSnapshot { history = List.copyOf(history); }
}
