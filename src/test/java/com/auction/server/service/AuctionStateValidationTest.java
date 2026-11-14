package com.auction.server.service;

import static com.auction.shared.JsonProtocol.obj;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.server.dao.AuctionRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuctionStateValidationTest {
  @TempDir Path directory;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "schemaVersion",
        "version",
        "users",
        "auctions",
        "rules",
        "commands",
        "transactions"
      })
  void incompleteSnapshotFailsStartupWithoutResetAndReleasesLock(String missing) throws Exception {
    JsonObject state =
        obj(
            "schemaVersion",
            1,
            "version",
            0,
            "users",
            obj(),
            "auctions",
            obj(),
            "rules",
            obj(),
            "commands",
            obj(),
            "transactions",
            new JsonArray());
    state.remove(missing);
    Path file = directory.resolve("state.json");
    Files.writeString(file, state.toString());
    byte[] original = Files.readAllBytes(file);
    assertThrows(
        IOException.class,
        () ->
            new AuctionService(directory, Clock.system(AuctionService.ZONE), true, ignored -> {}));
    assertArrayEquals(original, Files.readAllBytes(file));
    assertFalse(Files.exists(directory.resolve("state.json.tmp")));
    try (var repository = new AuctionRepository(directory)) {
      assertEquals(state, repository.load());
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "negativeBalance",
        "stringBalance",
        "invalidBoolean",
        "userId",
        "role",
        "auctionTime",
        "auctionStep",
        "sellerReference",
        "bidCount",
        "bid",
        "rule",
        "command",
        "transaction"
      })
  void corruptNestedDataFailsBeforeAnySchedulerOrWrite(String corruption) throws Exception {
    try (var seed = new AuctionService(directory, true)) {
      // Seed only the isolated fixture directory, never the application data directory.
    }
    Path file = directory.resolve("state.json");
    JsonObject state = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    JsonObject user =
        state.getAsJsonObject("users").entrySet().iterator().next().getValue().getAsJsonObject();
    JsonObject auction =
        state.getAsJsonObject("auctions").entrySet().iterator().next().getValue().getAsJsonObject();
    switch (corruption) {
      case "negativeBalance" -> user.addProperty("balance", -1);
      case "stringBalance" -> user.addProperty("balance", "10000");
      case "invalidBoolean" -> user.addProperty("locked", "false");
      case "userId" -> user.addProperty("id", "different-id");
      case "role" -> user.addProperty("role", "ROOT");
      case "auctionTime" -> auction.addProperty("endTime", "bad-time");
      case "auctionStep" -> auction.addProperty("minBidStep", -1);
      case "sellerReference" -> auction.addProperty("sellerId", "missing-user");
      case "bidCount" -> auction.addProperty("totalBids", 10);
      case "bid" -> auction.getAsJsonArray("bids").add(obj("amount", 1));
      case "rule" -> state.getAsJsonObject("rules").add("broken", obj());
      case "command" -> state.getAsJsonObject("commands").add("broken", obj());
      case "transaction" -> state.getAsJsonArray("transactions").add(obj());
      default -> throw new AssertionError(corruption);
    }
    Files.writeString(file, state.toString());
    byte[] original = Files.readAllBytes(file);
    assertThrows(IOException.class, () -> new AuctionService(directory, true));
    assertArrayEquals(original, Files.readAllBytes(file));
    assertFalse(Files.exists(directory.resolve("state.json.tmp")));
    try (var repository = new AuctionRepository(directory)) {
      assertEquals(state, repository.load(), "Startup failure must release the data lock");
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "unfundedHold",
        "missingLeader",
        "wrongCurrentPrice",
        "duplicateBid",
        "settledRunning",
        "duplicateTransaction",
        "commandOutcome"
      })
  void inconsistentFinancialSnapshotIsRejectedWithoutChangingBytes(String corruption)
      throws Exception {
    String auctionId;
    try (var seed = new AuctionService(directory, true)) {
      String token =
          seed.execute("LOGIN", "", "", obj("username", "bidder_hieu", "password", "Bidder12345"))
              .getAsJsonObject("result")
              .get("token")
              .getAsString();
      auctionId =
          seed.execute("LIST_AUCTIONS", "", "", obj())
              .getAsJsonArray("result")
              .get(0)
              .getAsJsonObject()
              .get("id")
              .getAsString();
      assertTrue(
          seed.execute(
                  "PLACE_BID",
                  token,
                  "fixture-bid-command",
                  obj("auctionId", auctionId, "amount", 2_000_000))
              .get("success")
              .getAsBoolean());
    }
    Path file = directory.resolve("state.json");
    JsonObject state = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    JsonObject auction = state.getAsJsonObject("auctions").getAsJsonObject(auctionId);
    switch (corruption) {
      case "unfundedHold" ->
          state
              .getAsJsonObject("users")
              .getAsJsonObject(auction.get("highestBidderId").getAsString())
              .addProperty("balance", 0);
      case "missingLeader" -> auction.addProperty("highestBidderId", "missing-user");
      case "wrongCurrentPrice" -> auction.addProperty("currentPrice", 3_000_000);
      case "duplicateBid" -> {
        JsonArray bids = auction.getAsJsonArray("bids");
        bids.add(bids.get(0).deepCopy());
        auction.addProperty("totalBids", 2);
      }
      case "settledRunning" -> auction.addProperty("settled", true);
      case "duplicateTransaction" -> {
        JsonArray transactions = state.getAsJsonArray("transactions");
        transactions.add(transactions.get(0).deepCopy());
      }
      case "commandOutcome" ->
          state
              .getAsJsonObject("commands")
              .entrySet()
              .iterator()
              .next()
              .getValue()
              .getAsJsonObject()
              .getAsJsonObject("response")
              .addProperty("success", false);
      default -> throw new AssertionError(corruption);
    }
    Files.writeString(file, state.toString());
    byte[] original = Files.readAllBytes(file);
    assertThrows(IOException.class, () -> new AuctionService(directory, true));
    assertArrayEquals(original, Files.readAllBytes(file));
    assertFalse(Files.exists(directory.resolve("state.json.tmp")));
    try (var repository = new AuctionRepository(directory)) {
      assertEquals(state, repository.load());
    }
  }
}
