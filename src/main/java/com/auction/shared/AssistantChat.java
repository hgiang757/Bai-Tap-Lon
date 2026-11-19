package com.auction.shared;

/** Wire action and input limit shared by the client and server. */
public final class AssistantChat {
  public static final String ACTION = "AI_CHAT_REQUEST";
  public static final int MAX_QUESTION_LENGTH = 1_000;

  private AssistantChat() {}

  /** Chat response using the auction price observed when the question was accepted. */
  public record Reply(String answer, long currentPrice, boolean hasBids, String marketSummary) {}
}
