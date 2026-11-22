package com.auction.client.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/** Tiện ích định dạng dữ liệu hiển thị chuẩn tiếng Việt. */
public final class FormatUtil {

  private static final DecimalFormat CURRENCY_FORMAT;
  private static final DecimalFormat COMPACT_FORMAT;
  private static final Pattern VND_INPUT =
      Pattern.compile("[0-9]+|[0-9]{1,3}([., ])[0-9]{3}(?:\\1[0-9]{3})*");
  private static final DateTimeFormatter CHART_TIME_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm", new Locale("vi", "VN"));

  static {
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
    symbols.setGroupingSeparator('.');
    CURRENCY_FORMAT = new DecimalFormat("#,##0", symbols);
    COMPACT_FORMAT = new DecimalFormat("0.#", symbols);
  }

  private FormatUtil() {}

  /** Parses a positive whole amount with optional, consistently grouped thousands. */
  public static long parsePositiveVnd(String text) {
    String amount = text == null ? "" : text.strip();
    if (!VND_INPUT.matcher(amount).matches()) {
      throw new NumberFormatException("Expected a whole VND amount");
    }
    long value = Long.parseLong(amount.replaceAll("[., ]", ""));
    if (value <= 0) {
      throw new NumberFormatException("Expected a positive VND amount");
    }
    return value;
  }

  /** Định dạng số tiền theo quy chuẩn Việt Nam: 185.000.000 ₫. */
  public static synchronized String formatCurrency(long amount) {
    return CURRENCY_FORMAT.format(amount) + " ₫";
  }

  /** Formats an amount in Vietnamese currency notation. */
  public static synchronized String formatCurrency(double amount) {
    return CURRENCY_FORMAT.format(amount) + " ₫";
  }

  /** Formats a compact price label for the chart axis. */
  public static synchronized String formatCompactPriceAxis(double amount) {
    double absoluteAmount = Math.abs(amount);
    if (absoluteAmount >= 1_000_000_000) {
      return COMPACT_FORMAT.format(amount / 1_000_000_000) + " tỷ";
    }
    if (absoluteAmount >= 1_000_000) {
      return COMPACT_FORMAT.format(amount / 1_000_000) + " tr";
    }
    return CURRENCY_FORMAT.format(amount);
  }

  /** Formats a timestamp as a chart time label. */
  public static String formatChartTime(double epochMillis) {
    return Instant.ofEpochMilli((long) epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(CHART_TIME_FORMAT);
  }

  /** Formats a timestamp as a chart time label. */
  public static String formatChartTime(LocalDateTime time) {
    return time.format(CHART_TIME_FORMAT);
  }

  /** Masks part of a participant name for compact bid history display. */
  public static String maskUsername(String username) {
    if (username == null || username.isEmpty()) {
      return "Người dùng";
    }
    return username.charAt(0)
        + "***"
        + (username.length() > 2 ? username.substring(username.length() - 1) : "");
  }
}
