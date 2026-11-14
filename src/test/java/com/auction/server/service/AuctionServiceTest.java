package com.auction.server.service;

import static com.auction.shared.JsonProtocol.obj;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuctionServiceTest {
  @TempDir Path directory;
  AuctionService app;
  MutableClock clock = new MutableClock();
  String buyer;
  String seller;
  String admin;
  String other;
  List<JsonObject> events = new ArrayList<>();

  static class MutableClock extends Clock {
    Instant instant = Instant.parse("2026-11-05T00:00:00Z");

    public ZoneId getZone() {
      return AuctionService.ZONE;
    }

    public Clock withZone(ZoneId z) {
      return this;
    }

    public Instant instant() {
      return instant;
    }

    void advance(long seconds) {
      instant = instant.plusSeconds(seconds);
    }
  }

  @BeforeEach
  void setup() throws Exception {
    app = new AuctionService(directory, clock, true, events::add);
    buyer = login("bidder_hieu", "Bidder12345");
    seller = login("seller_hieu", "Seller12345");
    admin = login("admin_hieu", "Admin12345");
    other = login("bidder_vip99", "Bidder12345");
  }

  @AfterEach
  void close() throws Exception {
    if (app != null) {
      app.close();
    }
  }

  String login(String name, String password) {
    return ok("LOGIN", "", obj("username", name, "password", password))
        .getAsJsonObject()
        .get("token")
        .getAsString();
  }

  JsonObject raw(String action, String token, JsonObject p) {
    return app.execute(action, token, UUID.randomUUID().toString(), p);
  }

  JsonElement ok(String action, String token, JsonObject p) {
    JsonObject r = raw(action, token, p);
    assertTrue(r.get("success").getAsBoolean(), r.toString());
    return r.get("result");
  }

  void fail(String code, String action, String token, JsonObject p) {
    assertEquals(code, raw(action, token, p).get("errorCode").getAsString());
  }

  String running() {
    return ok("LIST_AUCTIONS", "", obj())
        .getAsJsonArray()
        .get(0)
        .getAsJsonObject()
        .get("id")
        .getAsString();
  }

  JsonObject bid(String id, long amount) {
    return obj("auctionId", id, "amount", amount);
  }

  long available(String token) {
    return ok("GET_MY_WALLET_SNAPSHOT", token, obj())
        .getAsJsonObject()
        .get("available")
        .getAsLong();
  }

  JsonObject product(long delay, long duration) {
    LocalDateTime n = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    return obj(
        "name",
        "Sản phẩm mới",
        "description",
        "Mô tả",
        "category",
        "ART",
        "startingPrice",
        1_000_000,
        "minBidStep",
        100_000,
        "startTime",
        n.plusSeconds(delay),
        "endTime",
        n.plusSeconds(delay + duration));
  }

  @Test
  void credentialsAreHashedAndPublicDtosAreSafe() throws Exception {
    String persisted = Files.readString(directory.resolve("state.json"));
    assertFalse(persisted.contains("Bidder12345"));
    assertFalse(ok("GET_ME", buyer, obj()).getAsJsonObject().has("passwordHash"));
    fail("AUTHENTICATION_FAILED", "LOGIN", "", obj("username", "bidder_hieu", "password", "bad"));
    fail("UNAUTHENTICATED", "PLACE_BID", "", bid(running(), 2_000_000));
  }

  @Test
  void registerValidatesRoleUniquenessAndPassword() throws Exception {
    JsonObject p =
        obj(
            "username",
            "newuser",
            "password",
            "Password123",
            "email",
            "new@auction.vn",
            "fullName",
            "New User",
            "role",
            "ADMIN");
    fail("FORBIDDEN", "REGISTER", "", p);
    p.addProperty("role", "BIDDER");
    ok("REGISTER", "", p);
    fail("CONFLICT", "REGISTER", "", p);
    assertNotNull(login("newuser", "Password123"));
    assertFalse(Files.readString(directory.resolve("state.json")).contains("Password123"));
  }

  @Test
  void walletHoldsOutbidReleaseAndWithdrawAreConsistent() {
    String id = running();
    ok("PLACE_BID", buyer, bid(id, 2_000_000));
    assertEquals(98_000_000, available(buyer));
    fail("INSUFFICIENT_FUNDS", "WITHDRAW_SIMULATED", buyer, obj("amount", 99_000_000));
    ok("PLACE_BID", other, bid(id, 3_000_000));
    assertEquals(100_000_000, available(buyer));
    ok("WITHDRAW_SIMULATED", buyer, obj("amount", 10_000));
    assertEquals(99_990_000, available(buyer));
  }

  @Test
  void firstBidRequiresStepAndIntegerAndSellerCannotBidOwnProduct() {
    String id = running();
    fail("BID_TOO_LOW", "PLACE_BID", buyer, bid(id, 1_000_000));
    fail("VALIDATION_ERROR", "PLACE_BID", buyer, obj("auctionId", id, "amount", 1_100_000.5));
    fail("FORBIDDEN", "PLACE_BID", seller, bid(id, 2_000_000));
    ok("PLACE_BID", buyer, bid(id, 1_100_000));
    ok("PLACE_BID", buyer, bid(id, 1_200_000));
  }

  @Test
  void scheduledAuctionsCannotReceiveEarlyBidsAndOpenAutomatically() {
    String id =
        ok("CREATE_PRODUCT_AUCTION", seller, product(60, 3600))
            .getAsJsonObject()
            .get("id")
            .getAsString();
    fail("AUCTION_NOT_STARTED", "PLACE_BID", buyer, bid(id, 2_000_000));
    clock.advance(60);
    app.tick();
    ok("PLACE_BID", buyer, bid(id, 2_000_000));
  }

  @Test
  void settlementPaysSellerExactlyOnceAndSurvivesRestart() throws Exception {
    String id = running();
    ok("PLACE_BID", buyer, bid(id, 2_000_000));
    clock.advance(7201);
    app.tick();
    app.tick();
    assertEquals(98_000_000, available(buyer));
    assertEquals(2_000_000, available(seller));
    assertEquals(
        "PAID",
        ok("GET_AUCTION", "", obj("auctionId", id)).getAsJsonObject().get("status").getAsString());
    app.close();
    app = new AuctionService(directory, clock, false, events::add);
    buyer = login("bidder_hieu", "Bidder12345");
    seller = login("seller_hieu", "Seller12345");
    app.tick();
    assertEquals(98_000_000, available(buyer));
    assertEquals(2_000_000, available(seller));
  }

  @Test
  void antiSnipingExtendsAndDoesNotSettleAtOldDeadline() {
    String id = running();
    clock.advance(7180);
    ok("PLACE_BID", buyer, bid(id, 2_000_000));
    clock.advance(21);
    app.tick();
    assertEquals(
        "RUNNING",
        ok("GET_AUCTION", "", obj("auctionId", id)).getAsJsonObject().get("status").getAsString());
    assertTrue(
        events.stream().anyMatch(e -> e.get("event").getAsString().equals("AUCTION_EXTENDED")));
  }

  @Test
  void canceledAuctionReleasesHoldAndStopsAutoBid() {
    String id = running();
    ok("SET_MY_AUTO_BID", buyer, obj("auctionId", id, "maxBid", 5_000_000, "increment", 100_000));
    ok("CANCEL_AUCTION", admin, obj("auctionId", id, "reason", "Test"));
    assertEquals(100_000_000, available(buyer));
    fail("AUCTION_CLOSED", "PLACE_BID", other, bid(id, 7_000_000));
    clock.advance(8000);
    app.tick();
    assertEquals(
        "CANCELED",
        ok("GET_AUCTION", "", obj("auctionId", id)).getAsJsonObject().get("status").getAsString());
  }

  @Test
  void autoBidRespectsLimitBudgetAndRegistrationOrder() {
    String id = running();
    ok("SET_MY_AUTO_BID", buyer, obj("auctionId", id, "maxBid", 2_000_000, "increment", 100_000));
    ok("SET_MY_AUTO_BID", other, obj("auctionId", id, "maxBid", 2_000_000, "increment", 100_000));
    JsonObject a = ok("GET_AUCTION", "", obj("auctionId", id)).getAsJsonObject();
    assertEquals("bidder_hieu", a.get("highestBidderUsername").getAsString());
    assertTrue(a.get("currentPrice").getAsLong() <= 2_000_000);
    fail(
        "INSUFFICIENT_FUNDS",
        "SET_MY_AUTO_BID",
        buyer,
        obj("auctionId", id, "maxBid", 500_000_000, "increment", 100_000));
  }

  @Test
  void earlierAutoBidCannotBlockAnAffordableRivalAfterItsBudgetChanges() {
    JsonArray auctions = ok("LIST_AUCTIONS", "", obj()).getAsJsonArray();
    String first = auctions.get(0).getAsJsonObject().get("id").getAsString();
    String second = auctions.get(1).getAsJsonObject().get("id").getAsString();
    ok(
        "SET_MY_AUTO_BID",
        buyer,
        obj("auctionId", first, "maxBid", 5_000_000, "increment", 100_000));
    ok("PLACE_BID", buyer, bid(second, 98_000_000));
    ok(
        "SET_MY_AUTO_BID",
        other,
        obj("auctionId", first, "maxBid", 5_000_000, "increment", 5_000_000));
    JsonObject result = ok("GET_AUCTION", "", obj("auctionId", first)).getAsJsonObject();
    assertEquals("bidder_vip99", result.get("highestBidderUsername").getAsString());
    assertEquals(5_000_000, result.get("currentPrice").getAsLong());
    assertEquals(2_000_000, available(buyer));
    assertEquals(295_000_000, available(other));
  }

  @Test
  void idempotencyRejectsChangedPayloadAndPersistsFinalOutcome() throws Exception {
    String id = running();
    String command = UUID.randomUUID().toString();
    JsonObject p = bid(id, 2_000_000);
    JsonObject r = app.execute("PLACE_BID", buyer, command, p);
    assertEquals(r, app.execute("PLACE_BID", buyer, command, p));
    assertEquals(
        "CONFLICT",
        app.execute("PLACE_BID", buyer, command, bid(id, 3_000_000))
            .get("errorCode")
            .getAsString());
    app.close();
    app = new AuctionService(directory, clock, false, events::add);
    buyer = login("bidder_hieu", "Bidder12345");
    assertEquals(
        "SUCCEEDED",
        ok("GET_COMMAND_STATUS", buyer, obj("commandId", command))
            .getAsJsonObject()
            .get("status")
            .getAsString());
    assertEquals(1, ok("GET_BID_HISTORY", "", obj("auctionId", id)).getAsJsonArray().size());
  }

  @Test
  void concurrentAuctionsCannotOverspendOneWallet() throws Exception {
    JsonArray list = ok("LIST_AUCTIONS", "", obj()).getAsJsonArray();
    try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
      Future<JsonObject> one =
          pool.submit(
              () ->
                  raw(
                      "PLACE_BID",
                      buyer,
                      bid(list.get(0).getAsJsonObject().get("id").getAsString(), 80_000_000)));
      Future<JsonObject> two =
          pool.submit(
              () ->
                  raw(
                      "PLACE_BID",
                      buyer,
                      bid(list.get(1).getAsJsonObject().get("id").getAsString(), 80_000_000)));
      assertNotEquals(one.get().get("success"), two.get().get("success"));
      assertEquals(20_000_000, available(buyer));
    }
  }

  @Test
  void equalConcurrentBidsChooseOneLeaderAndRejectTheOtherAtTheNewPrice() throws Exception {
    String id = running();
    CountDownLatch start = new CountDownLatch(1);
    List<JsonObject> results = new ArrayList<>();
    try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<JsonObject>> futures = new ArrayList<>();
      for (String token : List.of(buyer, other)) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return raw("PLACE_BID", token, bid(id, 1_200_000));
                }));
      }
      start.countDown();
      for (Future<JsonObject> future : futures) {
        results.add(future.get(5, TimeUnit.SECONDS));
      }
    }
    assertEquals(1, results.stream().filter(r -> r.get("success").getAsBoolean()).count());
    JsonObject rejected =
        results.stream().filter(r -> !r.get("success").getAsBoolean()).findFirst().orElseThrow();
    assertEquals("BID_TOO_LOW", rejected.get("errorCode").getAsString());
    assertEquals(1, ok("GET_BID_HISTORY", "", obj("auctionId", id)).getAsJsonArray().size());
    assertEquals(398_800_000, available(buyer) + available(other));
    assertEquals(
        1_200_000,
        ok("GET_AUCTION", "", obj("auctionId", id))
            .getAsJsonObject()
            .get("currentPrice")
            .getAsLong());
  }

  @Test
  void adminLockAppliesToExistingSessionAndCannotBeSpoofed() {
    String id = ok("GET_ME", buyer, obj()).getAsJsonObject().get("id").getAsString();
    fail("FORBIDDEN", "LOCK_USER", buyer, obj("userId", id));
    ok("LOCK_USER", admin, obj("userId", id));
    fail("UNAUTHENTICATED", "DEPOSIT_SIMULATED", buyer, obj("amount", 10000));
    ok("UNLOCK_USER", admin, obj("userId", id));
    ok("DEPOSIT_SIMULATED", buyer, obj("amount", 10000));
  }

  @Test
  void productCrudChecksOwnerAndPreservesAuctionHistory() {
    JsonObject p = product(3600, 3600);
    String id = ok("CREATE_PRODUCT_AUCTION", seller, p).getAsJsonObject().get("id").getAsString();
    p.addProperty("auctionId", id);
    p.addProperty("name", "Tên sửa");
    ok("UPDATE_PRODUCT_AUCTION", seller, p);
    fail("FORBIDDEN", "DELETE_PRODUCT_AUCTION", buyer, obj("auctionId", id));
    ok("DELETE_PRODUCT_AUCTION", seller, obj("auctionId", id));
    fail("NOT_FOUND", "GET_AUCTION", "", obj("auctionId", id));
  }

  @Test
  void storageFailureRollsBackMoneyAndCommandResult() throws Exception {
    Path temporary = directory.resolve("state.json.tmp");
    Files.createDirectory(temporary);
    fail("STORAGE_ERROR", "DEPOSIT_SIMULATED", buyer, obj("amount", 10000));
    Files.delete(temporary);
    assertEquals(100_000_000, available(buyer));
  }

  @Test
  void failedBidDoesNotPublishOrPersistAndSameCommandCanBeRetried() throws Exception {
    String id = running();
    String command = UUID.randomUUID().toString();
    final byte[] before = Files.readAllBytes(directory.resolve("state.json"));
    events.clear();
    final Path temporary = Files.createDirectory(directory.resolve("state.json.tmp"));
    JsonObject result = app.execute("PLACE_BID", buyer, command, bid(id, 2_000_000));
    assertFalse(result.get("success").getAsBoolean());
    assertEquals("STORAGE_ERROR", result.get("errorCode").getAsString());
    assertTrue(events.isEmpty(), "A failed commit must not broadcast a successful bid");
    assertArrayEquals(before, Files.readAllBytes(directory.resolve("state.json")));
    assertEquals(100_000_000, available(buyer));
    assertEquals(0, ok("GET_BID_HISTORY", "", obj("auctionId", id)).getAsJsonArray().size());
    assertEquals(
        "NOT_FOUND",
        ok("GET_COMMAND_STATUS", buyer, obj("commandId", command))
            .getAsJsonObject()
            .get("status")
            .getAsString());
    Files.delete(temporary);
    assertTrue(
        app.execute("PLACE_BID", buyer, command, bid(id, 2_000_000)).get("success").getAsBoolean());
    assertEquals(98_000_000, available(buyer));
    app.close();
    app = new AuctionService(directory, clock, false, events::add);
    buyer = login("bidder_hieu", "Bidder12345");
    assertTrue(
        app.execute("PLACE_BID", buyer, command, bid(id, 2_000_000)).get("success").getAsBoolean());
    assertEquals(1, ok("GET_BID_HISTORY", "", obj("auctionId", id)).getAsJsonArray().size());
    assertEquals(98_000_000, available(buyer));
  }

  @Test
  void noBidAuctionFinishesWithoutPaymentAndInvalidImagesAreRejected() {
    JsonObject p = product(0, 60);
    p.addProperty("imageBase64", "aW52YWxpZA==");
    fail("VALIDATION_ERROR", "CREATE_PRODUCT_AUCTION", seller, p);
    p.remove("imageBase64");
    String id = ok("CREATE_PRODUCT_AUCTION", seller, p).getAsJsonObject().get("id").getAsString();
    clock.advance(61);
    app.tick();
    assertEquals(
        "FINISHED",
        ok("GET_AUCTION", "", obj("auctionId", id)).getAsJsonObject().get("status").getAsString());
  }

  @Test
  void extensionFieldsSurviveAutomaticBidsSettlementAndRestart() throws Exception {
    String id = running();
    ok("PLACE_BID", buyer, bid(id, 2_000_000));
    ok("SET_MY_AUTO_BID", buyer, obj("auctionId", id, "maxBid", 3_000_000, "increment", 100_000));
    String buyerId = app.authenticatedUserId(buyer);
    app.close();
    Path file = directory.resolve("state.json");
    JsonObject state = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    JsonObject extension = obj("preserve", new JsonArray(), "value", 9_000_000_000_001L);
    state.add("futureRoot", extension.deepCopy());
    state.getAsJsonObject("users").getAsJsonObject(buyerId).add("futureUser", extension.deepCopy());
    JsonObject auction = state.getAsJsonObject("auctions").getAsJsonObject(id);
    auction.add("futureAuction", extension.deepCopy());
    auction.getAsJsonArray("bids").get(0).getAsJsonObject().add("futureBid", extension.deepCopy());
    state
        .getAsJsonObject("rules")
        .getAsJsonObject(id + ":" + buyerId)
        .add("futureRule", extension.deepCopy());
    Files.writeString(file, state.toString());
    app = new AuctionService(directory, clock, false, events::add);
    buyer = login("bidder_hieu", "Bidder12345");
    other = login("bidder_vip99", "Bidder12345");
    ok("SET_MY_AUTO_BID", buyer, obj("auctionId", id, "maxBid", 3_500_000, "increment", 100_000));
    ok("PLACE_BID", other, bid(id, 2_100_000));
    assertEquals(
        "bidder_hieu",
        ok("GET_AUCTION", "", obj("auctionId", id))
            .getAsJsonObject()
            .get("highestBidderUsername")
            .getAsString());
    clock.advance(7201);
    app.tick();
    app.close();
    app = new AuctionService(directory, clock, false, events::add);
    buyer = login("bidder_hieu", "Bidder12345");
    seller = login("seller_hieu", "Seller12345");
    assertEquals(97_800_000, available(buyer));
    assertEquals(2_200_000, available(seller));
    JsonObject saved = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    assertEquals(1, saved.get("schemaVersion").getAsInt());
    assertEquals(extension, saved.get("futureRoot"));
    assertEquals(
        extension, saved.getAsJsonObject("users").getAsJsonObject(buyerId).get("futureUser"));
    JsonObject savedAuction = saved.getAsJsonObject("auctions").getAsJsonObject(id);
    assertEquals(extension, savedAuction.get("futureAuction"));
    assertEquals(
        extension, savedAuction.getAsJsonArray("bids").get(0).getAsJsonObject().get("futureBid"));
    assertEquals(
        extension,
        saved.getAsJsonObject("rules").getAsJsonObject(id + ":" + buyerId).get("futureRule"));
    assertEquals(3, savedAuction.get("totalBids").getAsInt());
  }

  @Test
  void failedSettlementSaveRestoresBothWalletsAndRetryPaysExactlyOnce() throws Exception {
    String id = running();
    ok("PLACE_BID", buyer, bid(id, 2_000_000));
    final byte[] before = Files.readAllBytes(directory.resolve("state.json"));
    events.clear();
    final Path temporary = Files.createDirectory(directory.resolve("state.json.tmp"));
    clock.advance(7201);
    fail("STORAGE_ERROR", "PING", "", obj());
    assertTrue(events.isEmpty());
    assertArrayEquals(before, Files.readAllBytes(directory.resolve("state.json")));
    Files.delete(temporary);
    app.tick();
    app.tick();
    assertEquals(98_000_000, available(buyer));
    assertEquals(2_000_000, available(seller));
    JsonArray buyerHistory = ok("LIST_MY_TRANSACTIONS", buyer, obj()).getAsJsonArray();
    JsonArray sellerHistory = ok("LIST_MY_TRANSACTIONS", seller, obj()).getAsJsonArray();
    assertEquals(
        1,
        buyerHistory.asList().stream()
            .filter(t -> t.getAsJsonObject().get("type").getAsString().equals("PAYMENT"))
            .count());
    assertEquals(
        1,
        sellerHistory.asList().stream()
            .filter(t -> t.getAsJsonObject().get("type").getAsString().equals("SALE"))
            .count());
  }

  @Test
  void rejectedProductUpdateDoesNotLeaveHelpersUsingTheDiscardedSnapshot() {
    JsonObject p = product(3600, 3600);
    String id = ok("CREATE_PRODUCT_AUCTION", seller, p).getAsJsonObject().get("id").getAsString();
    p.addProperty("auctionId", id);
    p.addProperty("name", "Must roll back");
    p.addProperty("imageBase64", "invalid-base64");
    fail("VALIDATION_ERROR", "UPDATE_PRODUCT_AUCTION", seller, p);
    assertEquals(
        "Sản phẩm mới",
        ok("GET_AUCTION", "", obj("auctionId", id)).getAsJsonObject().get("name").getAsString());
    clock.advance(3600);
    app.tick();
    ok("PLACE_BID", buyer, bid(id, 1_100_000));
    assertEquals(98_900_000, available(buyer));
    assertEquals(1, ok("GET_BID_HISTORY", "", obj("auctionId", id)).getAsJsonArray().size());
  }
}
