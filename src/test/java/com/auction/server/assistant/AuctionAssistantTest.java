package com.auction.server.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.server.exception.ApiException;
import com.auction.shared.AssistantChat;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AuctionAssistantTest {
  private static final Instant TIME = Instant.parse("2026-11-19T08:00:00Z");
  private static final Clock CLOCK = Clock.fixed(TIME, ZoneOffset.UTC);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final LocalDateTime START = LocalDateTime.of(2026, 11, 19, 14, 0);
  private static final LocalDateTime END = START.plusHours(2);
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
          1_600_000,
          3,
          true,
          START,
          END,
          List.of());

  @Test
  void missingKeyDoesNotContactProvider() throws Exception {
    try (MockGemini provider = new MockGemini(request -> MockGemini.answer("Unexpected"));
        AuctionAssistant assistant =
            provider.assistant(null, REQUEST_TIMEOUT, product -> Optional.empty(), CLOCK)) {
      assertEquals("AI_UNAVAILABLE", failure(assistant));
      var market = assistant.ask("bidder", PRODUCT, AssistantChat.MARKET_QUESTION, () -> {});
      assertEquals("Chưa có dữ liệu giá thị trường đã xác minh.", market.marketSummary());
      assertEquals(PRODUCT.currentPrice(), market.currentPrice());
      var room = assistant.ask("bidder", PRODUCT, AssistantChat.ROOM_QUESTION, () -> {});
      assertTrue(room.answer().contains(PRODUCT.name()));
      assertTrue(room.answer().contains("1.600.000"));
      assertTrue(room.answer().contains("Số lượt đặt: 3"));
      assertThrows(IllegalStateException.class,
          () -> assistant.ask("bidder", PRODUCT, AssistantChat.COMPARE_QUESTION,
              () -> {
                throw new IllegalStateException("Access revoked");
              }));
      assertNull(provider.nextRequest(Duration.ofMillis(100)));
    }
  }

  @Test
  void providerTimeoutAndRateLimitAreReportedSeparately() throws Exception {
    var hold = new CountDownLatch(1);
    try (MockGemini provider =
            new MockGemini(
                request -> {
                  hold.await();
                  return MockGemini.answer("Too late");
                });
        AuctionAssistant assistant = provider.assistant(Duration.ofMillis(500))) {
      assertEquals("AI_TIMEOUT", failure(assistant));
    } finally {
      hold.countDown();
    }
    try (MockGemini provider =
            new MockGemini(
                request -> new MockGemini.Response(429,
                    "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\"}}"));
        AuctionAssistant assistant = provider.assistant(REQUEST_TIMEOUT)) {
      assertEquals("AI_RATE_LIMIT", failure(assistant));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "{", "null", "{\"candidates\":{}}", "{\"candidates\":[]}"})
  void rejectsUnreadableOrEmptyProviderReplies(String body) throws Exception {
    try (MockGemini provider =
            new MockGemini(request -> new MockGemini.Response(200, body));
        AuctionAssistant assistant = provider.assistant(REQUEST_TIMEOUT)) {
      assertEquals("AI_PROVIDER_ERROR", failure(assistant));
      var reply = assistant.ask("bidder", PRODUCT, AssistantChat.ROOM_QUESTION, () -> {});
      assertEquals(PRODUCT.currentPrice(), reply.currentPrice());
    }
  }

  @ParameterizedTest
  @CsvSource({"400,AI_REQUEST_REJECTED", "401,AI_AUTH_FAILED", "403,AI_FORBIDDEN",
      "404,AI_MODEL_UNAVAILABLE", "407,AI_PROXY_ERROR", "500,AI_PROVIDER_BUSY",
      "502,AI_PROVIDER_BUSY", "504,AI_TIMEOUT"})
  void httpErrorsAreClassifiedWithoutRelayingProviderBody(int status, String code)
      throws Exception {
    String marker = java.util.UUID.randomUUID().toString();
    try (MockGemini provider = new MockGemini(request -> new MockGemini.Response(status, marker));
        AuctionAssistant assistant = provider.assistant(REQUEST_TIMEOUT)) {
      ApiException failure = assertThrows(ApiException.class,
          () -> assistant.ask("bidder", PRODUCT, "Câu hỏi", () -> {}));
      assertEquals(code, failure.code());
      assertFalse(failure.toString().contains(marker));
    }
  }

  @Test
  void stopsAfterOneRetryWhenGeminiStaysUnavailable() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    try (MockGemini provider = new MockGemini(request -> {
      attempts.incrementAndGet();
      return new MockGemini.Response(503, "{}");
    });
        AuctionAssistant assistant = provider.assistant(REQUEST_TIMEOUT)) {
      assertEquals("AI_PROVIDER_BUSY", failure(assistant));
      assertEquals(2, attempts.get());
    }
  }

  @Test
  void lostProviderConnectionDoesNotBecomeAnAnswerOrTriggerA503Retry() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    try (MockGemini provider = new MockGemini(request -> {
      attempts.incrementAndGet();
      throw new InterruptedException();
    });
        AuctionAssistant assistant = provider.assistant(REQUEST_TIMEOUT)) {
      assertEquals("AI_NETWORK_ERROR", failure(assistant));
      assertEquals(1, attempts.get());
      assertTrue(assistant.ask("bidder", PRODUCT, AssistantChat.ROOM_QUESTION, () -> {})
          .answer().contains(PRODUCT.name()));
    }
  }

  @Test
  void trimsPasteArtifactsAndRetriesOneExplicitTemporaryRejection() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    try (MockGemini provider = new MockGemini(request -> attempts.incrementAndGet() == 1
        ? new MockGemini.Response(503, "{}") : MockGemini.answer("Có dữ liệu phiên."));
        AuctionAssistant assistant = provider.assistant(
            "\u00A0\uFEFF \t" + provider.testHeader() + "\r\n \uFEFF",
            REQUEST_TIMEOUT,
            product -> Optional.empty(), CLOCK)) {
      var reply = assistant.ask("bidder", PRODUCT, "Câu hỏi", () -> {});
      assertEquals("Có dữ liệu phiên.", reply.answer());
      assertEquals(2, attempts.get());
      assertEquals(provider.testHeader(), provider.nextRequest(REQUEST_TIMEOUT).apiKey());
      assertEquals(provider.testHeader(), provider.nextRequest(REQUEST_TIMEOUT).apiKey());
    }
  }

  @Test
  void internalWhitespaceIsRejectedBeforeRequest() throws Exception {
    try (MockGemini provider = new MockGemini(request -> MockGemini.answer("Unexpected"));
        AuctionAssistant assistant = provider.assistant(provider.testHeader() + " extra",
            REQUEST_TIMEOUT, product -> Optional.empty(), CLOCK)) {
      assertEquals("AI_KEY_FORMAT", failure(assistant));
      assertNull(provider.nextRequest(Duration.ofMillis(100)));
    }
  }

  @ParameterizedTest
  @CsvSource(value = {
      "a\uFEFFb|U+FEFF", "a\u200Bb|U+200B", "\u001B[200~ab\u001B[201~|U+001B",
      "a\u001Cb|U+001C", "\u001Ckey|U+001C"
  }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = false)
  void reportsTheExactForbiddenCharacterBeforeSending(String input, String codePoint)
      throws Exception {
    try (MockGemini provider = new MockGemini(request -> MockGemini.answer("Unexpected"));
        AuctionAssistant assistant = provider.assistant(
            input, REQUEST_TIMEOUT, product -> Optional.empty(), CLOCK)) {
      ApiException error = assertThrows(ApiException.class,
          () -> assistant.ask("bidder", PRODUCT, "Câu hỏi", () -> {}));
      assertEquals("AI_KEY_FORMAT", error.code());
      assertTrue(error.getMessage().contains(codePoint));
      assertTrue(assistant.providerStatus().contains("Java HTTP preflight"));
      assertTrue(assistant.providerStatus().contains(codePoint));
      assertFalse(error.getMessage().contains(input));
      assertNull(provider.nextRequest(Duration.ofMillis(100)));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"********", "••••••••", "a\nb", "\u001B[200~bad"})
  void rejectsMaskOnlyWhitespaceAndInvisibleCharactersWithoutLeakingInput(String input)
      throws Exception {
    try (MockGemini provider = new MockGemini(request -> MockGemini.answer("Unexpected"));
        AuctionAssistant assistant = provider.assistant(input, REQUEST_TIMEOUT,
            product -> Optional.empty(), CLOCK)) {
      ApiException error = assertThrows(ApiException.class,
          () -> assistant.ask("bidder", PRODUCT, "Câu hỏi", () -> {}));
      assertEquals("AI_KEY_FORMAT", error.code());
      assertFalse(error.getMessage().contains(input));
      assertNull(provider.nextRequest(Duration.ofMillis(100)));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"x", "opaque*value", "\"opaque.value\"", "<opaque>", "YOUR_API_KEY"})
  void forwardsOpaqueHeaderWithoutChangingItsContent(String input) throws Exception {
    try (MockGemini provider = new MockGemini(request -> MockGemini.answer("Đã nhận."));
        AuctionAssistant assistant = provider.assistant(input, REQUEST_TIMEOUT,
            product -> Optional.empty(), CLOCK)) {
      assertEquals("Đã nhận.", assistant.ask("bidder", PRODUCT, "Câu hỏi", () -> {}).answer());
      assertEquals(input, provider.nextRequest(REQUEST_TIMEOUT).apiKey());
    }
  }

  @Test
  void marketQuestionUsesVerifiedQuoteWithoutCallingGemini() throws Exception {
    URI source = URI.create("https://market.example.com/camera");
    var quote =
        new MarketPriceService.Quote(
            1_700_000, 2_000_000, Currency.getInstance("VND"), source, TIME);
    try (MockGemini provider =
            new MockGemini(
                request -> MockGemini.answer("Giá khoảng 5 triệu: https://example.com"));
        AuctionAssistant assistant =
            provider.assistant(provider.testHeader(), REQUEST_TIMEOUT,
                product -> Optional.of(quote), CLOCK)) {
      var reply = assistant.ask("bidder", PRODUCT, AssistantChat.MARKET_QUESTION, () -> {});
      assertEquals(reply,
          assistant.ask("bidder", PRODUCT, "Giá thị trường là bao nhiêu?", () -> {}));
      assertFalse(reply.answer().contains("5 triệu"));
      assertFalse(reply.answer().contains("example.com"));
      assertTrue(reply.marketSummary().contains("1.700.000 VND"));
      assertNull(provider.nextRequest(Duration.ofMillis(100)));
    }
  }

  @Test
  void marketQuestionUsesAiWhenUnverifiedAndProviderIsReady() throws Exception {
    try (MockGemini provider =
            new MockGemini(
                request -> MockGemini.answer("Ước lượng tham khảo chưa xác minh: 2–3 triệu VND."));
        AuctionAssistant assistant = provider.assistant(REQUEST_TIMEOUT)) {
      var reply = assistant.ask("bidder", PRODUCT, AssistantChat.MARKET_QUESTION, () -> {});
      assertTrue(reply.answer().contains("2–3 triệu VND"));
      assertEquals("Chưa có dữ liệu giá thị trường đã xác minh.", reply.marketSummary());
      var captured = provider.nextRequest(REQUEST_TIMEOUT);
      assertEquals(provider.testHeader(), captured.apiKey());
      assertEquals(
          AssistantChat.MARKET_QUESTION,
          captured.context().get("question").getAsString());
    }
  }

  @Test
  void geminiEndpointUsesConfiguredModelOrSafeDefault() {
    assertTrue(AuctionAssistant.geminiEndpoint(null).toString()
        .endsWith("/models/gemini-3.5-flash-lite:generateContent"));
    assertTrue(AuctionAssistant.geminiEndpoint("  gemini-2.5-flash  ").toString()
        .endsWith("/models/gemini-2.5-flash:generateContent"));
    assertTrue(AuctionAssistant.geminiEndpoint("bad/model?x=1").toString()
        .endsWith("/models/gemini-3.5-flash-lite:generateContent"));
  }

  @Test
  void closedRoomUsesRecordedStatusAndDoesNotShowAnAvailableBid() throws Exception {
    var closed = new AuctionProduct("auction", "product", "Máy ảnh ABC", "ELECTRONICS",
        "", "", "FINISHED", 100, 100, 10, 110, 0, false, START, END, List.of());
    try (MockGemini provider = new MockGemini(request -> MockGemini.answer("Unexpected"));
        AuctionAssistant assistant = provider.assistant(REQUEST_TIMEOUT)) {
      var room = assistant.ask("bidder", closed, AssistantChat.ROOM_QUESTION, () -> {});
      assertTrue(room.answer().contains("Đã kết thúc"));
      assertTrue(room.answer().contains("Phiên hiện không nhận giá mới"));
      assertTrue(room.answer().contains("19/11/2026 16:00:00"));
      assertFalse(room.answer().contains("Mức đặt tối thiểu"));
      assertNull(provider.nextRequest(Duration.ofMillis(100)));
    }
  }

  @Test
  void freeFormQuestionUsesGeminiAndReturnsVisibleTextOnly() throws Exception {
    try (MockGemini provider = new MockGemini(request -> MockGemini.answer("Thông tin sản phẩm."));
        AuctionAssistant assistant = provider.assistant(REQUEST_TIMEOUT)) {
      var reply = assistant.ask("bidder", PRODUCT, "Sản phẩm có thông tin gì?", () -> {});
      assertEquals("Thông tin sản phẩm.", reply.answer());
      var request = provider.nextRequest(REQUEST_TIMEOUT);
      assertEquals(provider.testHeader(), request.apiKey());
      assertTrue(request.body().has("systemInstruction"));
      assertEquals(1024, request.body().getAsJsonObject("generationConfig")
          .get("maxOutputTokens").getAsInt());
      assertFalse(request.body().has("tools"));
      assertFalse(request.body().toString().contains(provider.testHeader()));
      var facts = request.context().getAsJsonObject("auctionProduct");
      assertEquals(3, facts.get("totalBids").getAsLong());
      assertTrue(facts.get("acceptsBids").getAsBoolean());
      assertEquals(
          reply.marketSummary(), request.context().get("verifiedMarketSummary").getAsString());
    }
  }

  @Test
  void verifiedMarketQuoteIncludesCurrencySourceTimeAndComparison() throws Exception {
    URI source = URI.create("https://market.example.com/camera");
    var quote =
        new MarketPriceService.Quote(
            1_700_000, 2_000_000, Currency.getInstance("VND"), source, TIME);
    try (MockGemini provider =
            new MockGemini(request -> MockGemini.answer("Hãy kiểm tra ống kính."));
        AuctionAssistant assistant =
            provider.assistant(provider.testHeader(), REQUEST_TIMEOUT,
                product -> Optional.of(quote), CLOCK)) {
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
    try (MockGemini provider =
            new MockGemini(request -> MockGemini.answer("Hãy kiểm tra ống kính."));
        AuctionAssistant assistant =
            provider.assistant(provider.testHeader(), REQUEST_TIMEOUT,
                product -> Optional.empty(), CLOCK)) {
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
