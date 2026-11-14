package com.auction.client.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Khóa lệnh phía client theo người dùng/phiên, tồn tại lâu hơn một màn hình. Token ở đây chỉ loại
 * callback cũ, KHÔNG phải request ID được server xác nhận.
 */
public final class BidSubmissionTracker {
  /** Tracks whether a bid is ready, sending, awaiting a refresh or still uncertain. */
  public enum Phase {
    READY,
    SENDING,
    AWAITING_REFRESH,
    UNKNOWN
  }

  /** Identifies bid state by account and auction rather than by the currently visible view. */
  public record Key(String username, String auctionId) {}

  /** Identifies one submission so late callbacks cannot complete a newer command. */
  public record Ticket(Key key, long command) {}

  /** Captures the tracker revision and phase when a snapshot read starts. */
  public record ReadStamp(Key key, long revision, Phase phase) {}

  private static final class Entry {
    long command;
    long revision;
    Phase phase = Phase.READY;
  }

  private final Map<Key, Entry> entries = new HashMap<>();
  private long sequence;

  private Entry entry(Key key) {
    return entries.computeIfAbsent(key, ignored -> new Entry());
  }

  /** Returns the current submission phase for an account and auction. */
  public synchronized Phase phase(Key key) {
    return entry(key).phase;
  }

  /** Only called after the adapter has reconciled the durable server command. */
  public synchronized void reconciled(Key key) {
    Entry e = entry(key);
    if (e.phase == Phase.UNKNOWN) {
      e.phase = Phase.AWAITING_REFRESH;
      e.revision = ++sequence;
    }
  }

  /** Starts a submission only when the account and auction are ready. */
  public synchronized Ticket begin(Key key) {
    Entry entry = entry(key);
    if (entry.phase != Phase.READY) {
      throw new IllegalStateException("Lượt đặt giá trước chưa được đối chiếu.");
    }
    entry.command = ++sequence;
    entry.revision = ++sequence;
    entry.phase = Phase.SENDING;
    return new Ticket(key, entry.command);
  }

  /** Requires a fresh snapshot after a definite command response. */
  public synchronized void answered(Ticket ticket) {
    finish(ticket, Phase.AWAITING_REFRESH);
  }

  /** Keeps a command unresolved until its server outcome can be recovered. */
  public synchronized void uncertain(Ticket ticket) {
    finish(ticket, Phase.UNKNOWN);
  }

  /** Restores readiness when a command was rejected before being submitted. */
  public synchronized void notSubmitted(Ticket ticket) {
    finish(ticket, Phase.READY);
  }

  private void finish(Ticket ticket, Phase phase) {
    Entry entry = entry(ticket.key());
    if (entry.command != ticket.command() || entry.phase != Phase.SENDING) {
      return;
    }
    entry.phase = phase;
    entry.revision = ++sequence;
  }

  /** Captures the submission revision before starting a snapshot read. */
  public synchronized ReadStamp reading(Key key) {
    Entry entry = entry(key);
    return new ReadStamp(key, entry.revision, entry.phase);
  }

  /** Checks that a read still belongs to the current submission revision. */
  public synchronized boolean isCurrent(ReadStamp stamp) {
    return entry(stamp.key()).revision == stamp.revision();
  }

  /** Chỉ đọc bắt đầu SAU phản hồi rõ ràng mới mở khóa. Snapshot không giải quyết UNKNOWN. */
  public synchronized void refreshed(ReadStamp stamp) {
    Entry entry = entry(stamp.key());
    if (isCurrent(stamp)
        && stamp.phase() == Phase.AWAITING_REFRESH
        && entry.phase == Phase.AWAITING_REFRESH) {
      entry.phase = Phase.READY;
      entry.revision = ++sequence;
    }
  }
}
