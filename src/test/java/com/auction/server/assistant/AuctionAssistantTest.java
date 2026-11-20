package com.auction.server.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.server.exception.ApiException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

class AuctionAssistantTest {
  private static final Instant TIME = Instant.parse("2026-11-19T08:00:00Z");
  private static final Clock CLOCK = Clock.fixed(TIME, ZoneOffset.UTC);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final AuctionProduct PRODUCT =
      new AuctionProduct(
          "auction",
          "product",
          "Máy ảnh ABC",
          "ELECTRONICS",
          "Máy đã qua sử dụng.",
          "Đã qua sử dụng",
          "RUNNING",
          1_000_000,
          1_500_000,
          100_000,
          true);

  @Test
  void missingKeyDoesNotContactProvider() throws Exception {
    try (MockOpenAi provider = new MockOpenAi(request -> MockOpenAi.answer("Unexpected"));
        AuctionAssistant assistant =
            provider.assistant(null, REQUEST_TIMEOUT, product -> Optional.empty(), CLOCK)) {
      assertEquals("AI_UNAVAILABLE", failure(assistant));
      assertNull(provider.nextRequest(Duration.ofMillis(100)));
    }
  }

  @Test
  void providerTimeoutAndQuotaAreReportedSeparately() throws Exception {
    var hold = new CountDownLatch(1);
    try (MockOpenAi provider =
            new MockOpenAi(
                request -> {
                  hold.await();
                  return MockOpenAi.answer("Too late");
                });
        AuctionAssistant assistant = provider.assistant(Duration.ofMillis(500))) {
      assertEquals("AI_TIMEOUT", failure(assistant));
    } finally {
      hold.countDown();
    }
    try (MockOpenAi provider =
            new MockOpenAi(
                request ->
                    new MockOpenAi.Response(429, "{\"error\":{\"code\":\"insufficient_quota\"}}"));
        AuctionAssistant assistant = provider.assistant(REQUEST_TIMEOUT)) {
      assertEquals("AI_QUOTA", failure(assistant));
    }
  }

  @Test
  void noMarketQuoteDoesNotShowProviderInventedPriceOrSource() throws Exception {
    try (MockOpenAi provider =
            new MockOpenAi(
                request -> MockOpenAi.answer("Giá khoảng 5 triệu: https://example.com"));
        AuctionAssistant assistant = provider.assistant(REQUEST_TIMEOUT)) {
      var reply = assistant.ask("bidder", PRODUCT, "Giá thị trường là bao nhiêu?", () -> {});
      assertFalse(reply.answer().contains("5 triệu"));
      assertFalse(reply.answer().contains("example.com"));
      assertEquals("Chưa có dữ liệu giá thị trường đã xác minh.", reply.marketSummary());
      var request = provider.nextRequest(REQUEST_TIMEOUT);
      assertEquals("Bearer mock-key", request.authorization());
      assertEquals("mock-model", request.body().get("model").getAsString());
      assertEquals(450, request.body().get("max_output_tokens").getAsInt());
      assertFalse(request.body().get("store").getAsBoolean());
      assertFalse(request.body().has("tools"));
      assertFalse(request.body().toString().contains("mock-key"));
      assertEquals(
          reply.marketSummary(), request.context().get("verifiedMarketSummary").getAsString());
    }
  }

  @Test
  void geminiUsesItsOwnHeaderAndReadsOnlyVisibleTextFromTheFallback() throws Exception {
    try (MockOpenAi provider =
            new MockOpenAi(
                request ->
                    request.path().contains("flash-lite")
                        ? new MockOpenAi.Response(404, "{}")
                        : new MockOpenAi.Response(
                            200,
                            """
                            {"candidates":[{"finishReason":"STOP","content":{"parts":[
                              {"thought":true,"text":"Giá khoảng 5 triệu"},
                              {"text":"Hãy kiểm tra ống kính."}
                            ]}}]}
                            """));
        AuctionAssistant assistant = provider.geminiAssistant(REQUEST_TIMEOUT)) {
      var reply = assistant.ask("bidder", PRODUCT, "Có gì cần kiểm tra?", () -> {});
      assertEquals("Hãy kiểm tra ống kính.", reply.answer());
      var primary = provider.nextRequest(REQUEST_TIMEOUT);
      var fallback = provider.nextRequest(REQUEST_TIMEOUT);
      assertTrue(primary.path().endsWith("/gemini-3.5-flash-lite:generateContent"));
      assertTrue(fallback.path().endsWith("/gemini-3.5-flash:generateContent"));
      assertEquals("mock-key", fallback.geminiKey());
      assertNull(fallback.authorization());
      assertEquals("Có gì cần kiểm tra?", fallback.context().get("question").getAsString());
    }
  }

  @Test
  void verifiedMarketQuoteIncludesCurrencySourceTimeAndComparison() throws Exception {
    URI source = URI.create("https://market.example.com/camera");
    var quote =
        new MarketPriceService.Quote(
            1_700_000, 2_000_000, Currency.getInstance("VND"), source, TIME);
    try (MockOpenAi provider =
            new MockOpenAi(request -> MockOpenAi.answer("Hãy kiểm tra ống kính."));
        AuctionAssistant assistant =
            provider.assistant("mock-key", REQUEST_TIMEOUT, product -> Optional.of(quote), CLOCK)) {
      var reply = assistant.ask("bidder", PRODUCT, "So sánh giá", () -> {});
      assertTrue(reply.marketSummary().contains("1.700.000 VND"));
      assertTrue(reply.marketSummary().contains(source.toString()));
      assertTrue(reply.marketSummary().contains("19/11/2026 15:00 UTC+07:00"));
      assertTrue(reply.marketSummary().contains("thấp hơn khoảng tham khảo"));
      assertEquals(
          reply.marketSummary(),
          provider
              .nextRequest(REQUEST_TIMEOUT)
              .context()
              .get("verifiedMarketSummary")
              .getAsString());
    }
  }

  @Test
  void capsHourlyRequestsAndRetainsOnlyThreeTurns() throws Exception {
    try (MockOpenAi provider =
            new MockOpenAi(request -> MockOpenAi.answer("Hãy kiểm tra ống kính."));
        AuctionAssistant assistant =
            provider.assistant("mock-key", REQUEST_TIMEOUT, product -> Optional.empty(), CLOCK)) {
      for (int turn = 0; turn < 12; turn++) {
        assistant.ask("bidder", PRODUCT, "Câu hỏi " + turn, () -> {});
        var history =
            provider.nextRequest(REQUEST_TIMEOUT).context().getAsJsonArray("recentConversation");
        assertEquals(Math.min(turn, 3), history.size());
        if (turn > 3) {
          assertEquals(
              "Câu hỏi " + (turn - 3),
              history.get(0).getAsJsonObject().get("question").getAsString());
        }
      }
      assertEquals("AI_USER_LIMIT", failure(assistant));
      assertNull(provider.nextRequest(Duration.ofMillis(100)));
    }
  }

  private static String failure(AuctionAssistant assistant) {
    return assertThrows(
            ApiException.class, () -> assistant.ask("bidder", PRODUCT, "Một câu hỏi", () -> {}))
        .code();
  }
}
