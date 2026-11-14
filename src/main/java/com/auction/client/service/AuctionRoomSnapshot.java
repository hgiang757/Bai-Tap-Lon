package com.auction.client.service;

import com.auction.client.model.Auction;
import com.auction.client.model.Bid;
import java.util.List;

/** Pairs an auction snapshot with an immutable copy of its bid history. */
public record AuctionRoomSnapshot(Auction auction, List<Bid> history) {
  /** Copies the supplied history so callers cannot change this snapshot. */
  public AuctionRoomSnapshot {
    history = List.copyOf(history);
  }
}
