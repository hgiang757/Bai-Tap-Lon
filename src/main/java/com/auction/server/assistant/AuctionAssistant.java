package com.auction.server.assistant;

import static com.auction.shared.JsonProtocol.obj;

import com.auction.server.exception.ApiException;
import com.auction.shared.AssistantChat.Reply;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

/** Trợ lý chỉ đọc, lưu lịch sử và giới hạn tần suất gọi trong bộ nhớ. */
public final class AuctionAssistant implements AutoCloseable {
  private static final String NO_QUOTE = "Chưa có dữ liệu giá thị trường đã xác minh.";
  private static final String GEMINI_ENDPOINT =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent";
  private static final String FREE_MODEL = "gemini-3.5-flash-lite";
  private static final Duration PROVIDER_TIMEOUT = Duration.ofSeconds(25);
  private static final int MAX_TURNS = 3;
  private static final int MAX_CONVERSATIONS = 256;
  private static final int PER_USER_HOUR = 12;
  private static final int PER_SERVER_DAY = 200;
  private static final Duration HISTORY_AGE = Duration.ofMinutes(30);
  private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
  private static final DateTimeFormatter QUOTE_TIME =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'XXX", VIETNAMESE)
          .withZone(ZoneOffset.ofHours(7));
  private static final Pattern GENERATED_PRICE_OR_LINK =
      Pattern.compile(
          "(?iu)(https?://|www\\.|\\b[\\p{L}0-9-]+\\.(vn|com|net|org)\\b"
              + "|[₫$€£]|(?<!\\p{L})(vnd|vnđ|usd|eur)(?!\\p{L})"
              + "|(?<![\\p{L}\\p{N}])(\\p{N}{5,}|\\p{N}{1,3}([., ]\\p{N}{3})+)(?![\\p{L}\\p{N}])"
              + "|(?<!\\p{L})(triệu|tỷ|tỉ|nghìn|ngàn)\\s+đồng(?!\\p{L})"
              + "|\\p{N}[\\p{N}\\s.,]*(đ|đồng|triệu|tỷ|tỉ|nghìn|ngàn|k)(?!\\p{L}))");

  private final OpenAiClient openAi;
  private final MarketPriceService marketPrices;
  private final Clock clock;
  private final LinkedHashMap<Conversation, History> histories =
      new LinkedHashMap<>(16, 0.75f, true);
  private final Map<String, Integer> userCalls = new HashMap<>();
  private final Set<String> activeUsers = new HashSet<>();
  private final Semaphore apiSlots = new Semaphore(2);
  private Instant userHour = Instant.MIN;
  private Instant budgetDay = Instant.MIN;
  private int serverCalls;

  private record Conversation(String userId, String auctionId, String productId) {}

  private record Turn(String question, String answer) {}

  private record History(ArrayDeque<Turn> turns, Instant updated) {}

  AuctionAssistant(OpenAiClient openAi, MarketPriceService marketPrices, Clock clock) {
    this.openAi = openAi;
    this.marketPrices = marketPrices;
    this.clock = clock;
  }

  /** Chọn nhà cung cấp theo khóa API được cấu hình trên máy chủ. */
  public static AuctionAssistant fromEnvironment() {
    String geminiKey = System.getenv("GEMINI_API_KEY");
    String openAiModel = System.getenv("OPENAI_MODEL");
    OpenAiClient provider =
        geminiKey != null && !geminiKey.isBlank()
            ? new OpenAiClient(geminiKey, FREE_MODEL, URI.create(GEMINI_ENDPOINT), PROVIDER_TIMEOUT)
            : new OpenAiClient(
                System.getenv("OPENAI_API_KEY"),
                openAiModel == null || openAiModel.isBlank() ? "gpt-4.1-mini" : openAiModel.strip(),
                URI.create("https://api.openai.com/v1/responses"),
                PROVIDER_TIMEOUT);
    return new AuctionAssistant(
        provider, product -> Optional.empty(), Clock.systemUTC());
  }

  /** Trả lời dựa trên trạng thái lúc nhận câu hỏi và kiểm tra quyền trước khi lưu hội thoại. */
  public Reply ask(String userId, AuctionProduct product, String question, Runnable verifyAccess)
      throws InterruptedException {
    openAi.requireConfigured();
    Conversation conversation = new Conversation(userId, product.auctionId(), product.productId());
    JsonArray history = admit(conversation);
    try {
      Optional<MarketPriceService.Quote> quote = marketPrices.findComparablePrices(product);
      String marketSummary = marketSummary(product, quote);
      JsonArray input = new JsonArray();
      input.add(
          obj(
              "role",
              "user",
              "content",
              obj(
                      "auctionProduct", productFacts(product),
                      "verifiedMarketSummary", marketSummary,
                      "recentConversation", history,
                      "question", question)
                  .toString()));
      if (!apiSlots.tryAcquire()) {
        throw new ApiException("AI_BUSY", "Assistant is busy with other requests");
      }
      String answer;
      try {
        answer = openAi.answer(input);
      } finally {
        apiSlots.release();
      }
      if (GENERATED_PRICE_OR_LINK.matcher(answer).find()) {
        answer = "Thông tin về giá và nguồn tham khảo được hiển thị bên dưới.";
      }
      verifyAccess.run();
      if (Thread.currentThread().isInterrupted()) {
        throw new InterruptedException();
      }
      remember(conversation, new Turn(question, answer));
      return new Reply(answer, product.currentPrice(), product.hasBids(), marketSummary);
    } finally {
      synchronized (this) {
        activeUsers.remove(userId);
      }
    }
  }

  private synchronized JsonArray admit(Conversation conversation) {
    Instant now = clock.instant();
    Instant hour = now.truncatedTo(ChronoUnit.HOURS);
    Instant day = now.truncatedTo(ChronoUnit.DAYS);
    if (hour.isAfter(userHour)) {
      userCalls.clear();
      userHour = hour;
    }
    if (day.isAfter(budgetDay)) {
      serverCalls = 0;
      budgetDay = day;
    }
    String userId = conversation.userId();
    if (activeUsers.contains(userId)) {
      throw new ApiException("AI_BUSY", "An assistant request is already running");
    }
    if (serverCalls >= PER_SERVER_DAY) {
      throw new ApiException("AI_DAILY_LIMIT", "Server assistant budget exhausted");
    }
    int used = userCalls.getOrDefault(userId, 0);
    if (used >= PER_USER_HOUR) {
      throw new ApiException("AI_USER_LIMIT", "Hourly assistant limit exceeded");
    }
    activeUsers.add(userId);
    userCalls.put(userId, used + 1);
    serverCalls++;

    JsonArray result = new JsonArray();
    histories.values().removeIf(history -> history.updated().plus(HISTORY_AGE).isBefore(now));
    History history = histories.get(conversation);
    if (history != null) {
      history
          .turns()
          .forEach(turn -> result.add(obj("question", turn.question(), "answer", turn.answer())));
    }
    return result;
  }

  private synchronized void remember(Conversation conversation, Turn turn) {
    History previous = histories.get(conversation);
    var turns = previous == null ? new ArrayDeque<Turn>() : previous.turns();
    turns.addLast(turn);
    if (turns.size() > MAX_TURNS) {
      turns.removeFirst();
    }
    histories.put(conversation, new History(turns, clock.instant()));
    if (histories.size() > MAX_CONVERSATIONS) {
      histories.pollFirstEntry();
    }
  }

  private static JsonObject productFacts(AuctionProduct product) {
    return obj(
        "name", product.name(),
        "category", product.category(),
        "sellerDescription", product.description(),
        "condition", product.condition(),
        "auctionStatus", product.status(),
        "startingPrice", product.startingPrice(),
        "currentPrice", product.currentPrice(),
        "minBidStep", product.minBidStep(),
        "hasBids", product.hasBids(),
        "currency", "VND");
  }

  private static String marketSummary(
      AuctionProduct product, Optional<MarketPriceService.Quote> result) {
    if (result.isEmpty()) {
      return NO_QUOTE;
    }
    var quote = result.get();
    String range =
        "%s %s – %s %s. Nguồn %s · Truy xuất %s."
            .formatted(
                formatAmount(quote.low()), quote.currency(),
                formatAmount(quote.high()), quote.currency(),
                quote.source(), QUOTE_TIME.format(quote.retrievedAt()));
    if (!"VND".equals(quote.currency().getCurrencyCode())) {
      return range + " Giá phiên dùng VND nên chưa thể so sánh trực tiếp.";
    }
    String relation =
        product.currentPrice() < quote.low()
            ? "thấp hơn khoảng tham khảo"
            : product.currentPrice() > quote.high()
                ? "cao hơn khoảng tham khảo"
                : "nằm trong khoảng tham khảo";
    return range + " Giá phiên tại lúc hỏi " + relation + ".";
  }

  private static String formatAmount(long amount) {
    return String.format(VIETNAMESE, "%,d", amount);
  }

  @Override
  public void close() {
    openAi.close();
  }
}
