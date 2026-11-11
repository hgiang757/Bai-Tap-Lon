package com.auction.server.service;

import com.auction.server.model.Auction;
import com.auction.server.model.Bid;

public interface AuctionUpdateListener {
    void onNewBid(Auction auction, Bid newBid);
    void onAuctionStatusChanged(Auction auction);
}
