package com.auction.server.network;

import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.client.model.AuctionStatus;
import com.auction.client.service.server.JsonTransport;
import com.auction.client.service.server.ServerServices;
import com.auction.server.assistant.MockGemini;
import com.auction.shared.AssistantChat;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AssistantTcpIntegrationTest {
  private static final Duration WAIT = Duration.ofSeconds(5);
  @TempDir Path directory;

  @Test
  void snapshotsUseLatestPriceAndKeepProductAndUserHistoriesSeparate() throws Exception {
    try (MockGemini provider = new MockGemini(request -> MockGemini.answer("Thông tin sản phẩm."));
        Server server = start(provider);
        ServerServices bidder = adapter(server, "bidder");
        ServerServices rival = adapter(server, "rival")) {
      assertTrue(bidder.login("bidder_hieu", "Bidder12345", null));
      assertTrue(rival.login("bidder_vip99", "Bidder12345", null));
      var products = bidder.getAuctions(AuctionStatus.RUNNING);
      var first = products.get(0);
      var firstReply = bidder.askAssistant(bidder.getCurrentUser(), first.getId(), "Chi tiết A");
      assertFalse(firstReply.hasBids());
      assertEquals(1_000_000, firstReply.currentPrice());
      var initial = provider.nextRequest(WAIT).context();
      assertEquals(first.getItemName(), text(initial.getAsJsonObject("auctionProduct"), "name"));
      assertTrue(initial.getAsJsonArray("recentConversation").isEmpty());
      assertFalse(initial.toString().contains("bidder_hieu"));
      assertFalse(initial.toString().contains("seller_hieu"));
      assertFalse(initial.toString().contains("balance"));

      assertTrue(rival.placeBid(first.getId(), 2_000_000).isSuccess());
      var updated =
          bidder.askAssistant(bidder.getCurrentUser(), first.getId(), "Chi tiết tiếp theo");
      assertTrue(updated.hasBids());
      assertEquals(2_000_000, updated.currentPrice());
      var followUp = provider.nextRequest(WAIT).context();
      assertEquals(
          2_000_000, followUp.getAsJsonObject("auctionProduct").get("currentPrice").getAsLong());
      var points = followUp.getAsJsonObject("auctionProduct").getAsJsonArray("recentBids");
      assertEquals(1, points.size());
      var point = points.get(0).getAsJsonObject();
      assertEquals(java.util.Set.of("amount", "timestamp"), point.keySet());
      assertEquals(2_000_000, point.get("amount").getAsLong());
      assertFalse(text(point, "timestamp").isBlank());
      assertFalse(followUp.toString().contains("bidder_vip99"));
      assertEquals(
          "Chi tiết A",
          text(followUp.getAsJsonArray("recentConversation").get(0).getAsJsonObject(), "question"));

      var second = products.get(1);
      bidder.askAssistant(bidder.getCurrentUser(), second.getId(), "Chi tiết B");
      var switched = provider.nextRequest(WAIT).context();
      assertEquals(second.getItemName(), text(switched.getAsJsonObject("auctionProduct"), "name"));
      assertTrue(switched.getAsJsonArray("recentConversation").isEmpty());

      rival.askAssistant(rival.getCurrentUser(), first.getId(), "Câu hỏi từ người khác");
      assertTrue(
          provider.nextRequest(WAIT).context().getAsJsonArray("recentConversation").isEmpty());
    }
  }

  @Test
  void twoClientsCanChatWhileTheSameConnectionPlacesBidAndReadsWallet() throws Exception {
    var release = new CountDownLatch(1);
    try (MockGemini provider =
            new MockGemini(
                request -> {
                  release.await();
                  return MockGemini.answer("Trả lời " + text(request.context(), "question"));
                });
        Server server = start(provider);
        ServerServices bidder = adapter(server, "bidder");
        ServerServices rival = adapter(server, "rival");
        var workers = Executors.newVirtualThreadPerTaskExecutor()) {
      assertTrue(bidder.login("bidder_hieu", "Bidder12345", null));
      assertTrue(rival.login("bidder_vip99", "Bidder12345", null));
      String id = bidder.getAuctions(AuctionStatus.RUNNING).getFirst().getId();
      var first = workers.submit(() -> bidder.askAssistant(bidder.getCurrentUser(), id, "A"));
      var second = workers.submit(() -> rival.askAssistant(rival.getCurrentUser(), id, "B"));
      try {
        assertTrue(provider.nextRequest(WAIT) != null);
        assertTrue(provider.nextRequest(WAIT) != null);
        assertTrue(bidder.placeBid(id, 2_000_000).isSuccess());
        assertEquals(98_000_000, bidder.getAvailable("bidder_hieu"), 0.01);
        assertFalse(first.isDone());
        assertFalse(second.isDone());
      } finally {
        release.countDown();
      }
      assertEquals("Trả lời A", first.get(5, TimeUnit.SECONDS).answer());
      assertEquals("Trả lời B", second.get(5, TimeUnit.SECONDS).answer());
    }
  }

  @Test
  void rejectsUnauthorizedArchivedAndInvalidRequestsBeforeHttp() throws Exception {
    try (MockGemini provider = new MockGemini(request -> MockGemini.answer("Unexpected"));
        Server server = start(provider);
        JsonTransport wire = new JsonTransport("127.0.0.1", server.port(), 5_000, event -> {})) {
      String token = login(wire, "seller_hieu", "Seller12345");
      var auctions = wire.call("LIST_AUCTIONS", token, "", obj()).getAsJsonArray("result");
      String auction = text(auctions.get(0).getAsJsonObject(), "id");
      assertEquals(
          "UNAUTHENTICATED",
          text(
              wire.call(AssistantChat.ACTION, "", "", question(auction, "Xin chào")), "errorCode"));
      assertEquals(
          "VALIDATION_ERROR",
          text(
              wire.call(AssistantChat.ACTION, token, "", obj("auctionId", auction, "question", 12)),
              "errorCode"));
      assertEquals(
          "VALIDATION_ERROR",
          text(
              wire.call(AssistantChat.ACTION, token, "", question(auction, "x".repeat(1_001))),
              "errorCode"));

      var now = java.time.LocalDateTime.now(com.auction.server.service.AuctionService.ZONE);
      var created =
          wire.call(
              "CREATE_PRODUCT_AUCTION",
              token,
              UUID.randomUUID().toString(),
              obj(
                  "name",
                  "Sản phẩm lưu trữ",
                  "category",
                  "OTHER",
                  "description",
                  "Một sản phẩm",
                  "startingPrice",
                  100_000,
                  "minBidStep",
                  10_000,
                  "startTime",
                  now.plusHours(1),
                  "endTime",
                  now.plusHours(2)));
      assertTrue(created.get("success").getAsBoolean(), created::toString);
      String archived = text(created.getAsJsonObject("result"), "id");
      var deleted =
          wire.call(
              "DELETE_PRODUCT_AUCTION",
              token,
              UUID.randomUUID().toString(),
              obj("auctionId", archived));
      assertTrue(deleted.get("success").getAsBoolean(), deleted::toString);
      assertEquals(
          "NOT_FOUND",
          text(
              wire.call(AssistantChat.ACTION, token, "", question(archived, "Xin chào")),
              "errorCode"));
      assertNull(provider.nextRequest(Duration.ofMillis(100)));
    }
  }

  @Test
  void socketDisconnectCancelsPendingWorkWithoutSavingItsTurn() throws Exception {
    var hold = new CountDownLatch(1);
    try (MockGemini provider =
            new MockGemini(
                request -> {
                  if ("Hỏi trước ngắt kết nối".equals(text(request.context(), "question"))) {
                    hold.await();
                  }
                  return MockGemini.answer("Thông tin sản phẩm.");
                });
        Server server = start(provider);
        JsonTransport original = new JsonTransport("127.0.0.1", server.port(), 5_000, event -> {});
        JsonTransport retry = new JsonTransport("127.0.0.1", server.port(), 5_000, event -> {});
        var worker = Executors.newVirtualThreadPerTaskExecutor()) {
      String token = login(original, "bidder_hieu", "Bidder12345");
      String id =
          text(
              original
                  .call("LIST_AUCTIONS", token, "", obj())
                  .getAsJsonArray("result")
                  .get(0)
                  .getAsJsonObject(),
              "id");
      var pending =
          worker.submit(
              () ->
                  original.call(
                      AssistantChat.ACTION, token, "", question(id, "Hỏi trước ngắt kết nối")));
      assertTrue(provider.nextRequest(WAIT) != null);
      original.disconnect();
      assertThrows(ExecutionException.class, () -> pending.get(2, TimeUnit.SECONDS));

      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
      JsonObject result;
      do {
        result =
            retry.call(AssistantChat.ACTION, token, "", question(id, "Hỏi sau khi kết nối lại"));
        if (!"AI_BUSY".equals(text(result, "errorCode"))) {
          break;
        }
        Thread.sleep(25);
      } while (System.nanoTime() < deadline);
      assertTrue(result.get("success").getAsBoolean(), result.toString());
      assertTrue(
          provider.nextRequest(WAIT).context().getAsJsonArray("recentConversation").isEmpty());
    } finally {
      hold.countDown();
    }
  }

  private Server start(MockGemini provider) throws Exception {
    Server server =
        TestServers.create(
            "127.0.0.1",
            0,
            directory.resolve("server"),
            true,
            provider.assistant(Duration.ofSeconds(12)));
    server.start();
    return server;
  }

  private ServerServices adapter(Server server, String name) {
    return new ServerServices("127.0.0.1", server.port(), directory.resolve(name));
  }

  private static String login(JsonTransport wire, String username, String password) {
    return text(
        wire.call("LOGIN", "", "", obj("username", username, "password", password))
            .getAsJsonObject("result"),
        "token");
  }

  private static JsonObject question(String auctionId, String question) {
    return obj("auctionId", auctionId, "question", question);
  }
}
