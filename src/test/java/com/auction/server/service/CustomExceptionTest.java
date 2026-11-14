package com.auction.server.service;

import static com.auction.shared.JsonProtocol.obj;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.auction.server.exception.AuctionClosedException;
import com.auction.server.exception.AuthenticationException;
import com.auction.server.exception.InvalidBidException;
import com.auction.server.model.Auction;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CustomExceptionTest {
  private final LocalDateTime start = LocalDateTime.of(2026, 11, 14, 10, 0);

  @Test
  void lowBidHasTheSpecificExceptionAndExistingCode() {
    var auction = new Auction("a", "seller", 100, 10, "RUNNING", start, start.plusHours(1));
    var error = assertThrows(InvalidBidException.class, () -> auction.requireMinimumBid(109));
    assertEquals("BID_TOO_LOW", error.code());
  }

  @Test
  void expiredAuctionHasTheSpecificExceptionAndExistingCode() {
    var auction = new Auction("a", "seller", 100, 10, "RUNNING", start, start.plusHours(1));
    var error =
        assertThrows(
            AuctionClosedException.class,
            () -> auction.requireBidder("buyer", "BIDDER", start.plusHours(1)));
    assertEquals("AUCTION_CLOSED", error.code());
  }

  @Test
  void unknownLoginHasTheSpecificExceptionAndExistingCode() {
    var context = new AuctionContext(Clock.systemUTC());
    context.state = obj("users", obj());
    var auth = new AuthLogic(context, new WalletLogic(context));
    var error =
        assertThrows(
            AuthenticationException.class,
            () -> auth.login(obj("username", "unknown", "password", "Wrong12345")));
    assertEquals("AUTHENTICATION_FAILED", error.code());
  }
}
