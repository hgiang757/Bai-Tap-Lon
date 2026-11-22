package com.auction.shared;

/** Wire action and input limit shared by the client and server. */
public final class AssistantChat {
  public static final String ACTION = "AI_CHAT_REQUEST";
  public static final int MAX_QUESTION_LENGTH = 1_000;
  public static final String ROOM_QUESTION = "Hãy tóm tắt dữ liệu phiên đấu giá hiện tại.";
  public static final String INTRO_QUESTION = "Hãy giới thiệu sản phẩm đang được đấu giá.";
  public static final String MARKET_QUESTION = "Sản phẩm này có giá thị trường đã xác minh không?";
  public static final String COMPARE_QUESTION =
      "Hãy so sánh giá đấu hiện tại với giá thị trường đã xác minh.";
  public static final String CHECKLIST_QUESTION =
      "Tôi cần kiểm tra những gì trước khi đấu giá sản phẩm này?";

  private AssistantChat() {}

  /** Chat response using the auction price observed when the question was accepted. */
  public record Reply(String answer, long currentPrice, boolean hasBids, String marketSummary) {}
}
