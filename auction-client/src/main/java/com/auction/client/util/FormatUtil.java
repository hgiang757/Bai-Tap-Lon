package com.auction.client.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Tiện ích định dạng dữ liệu hiển thị chuẩn tiếng Việt.
 */
public final class FormatUtil {

    private static final DecimalFormat CURRENCY_FORMAT;
    private static final DecimalFormat COMPACT_FORMAT;
    private static final DateTimeFormatter CHART_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm", new Locale("vi", "VN"));

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        CURRENCY_FORMAT = new DecimalFormat("#,##0", symbols);
        COMPACT_FORMAT = new DecimalFormat("0.#", symbols);
    }

    private FormatUtil() {
    }

    /**
     * Định dạng số tiền theo quy chuẩn Việt Nam: 185.000.000 ₫
     */
    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount) + " ₫";
    }

    public static String formatCompactPriceAxis(double amount) {
        double absoluteAmount = Math.abs(amount);
        if (absoluteAmount >= 1_000_000_000) {
            return COMPACT_FORMAT.format(amount / 1_000_000_000) + " tỷ";
        }
        if (absoluteAmount >= 1_000_000) {
            return COMPACT_FORMAT.format(amount / 1_000_000) + " tr";
        }
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatChartTime(double epochMillis) {
        return Instant.ofEpochMilli((long) epochMillis).atZone(ZoneId.systemDefault())
                .format(CHART_TIME_FORMAT);
    }

    public static String formatChartTime(LocalDateTime time) {
        return time.format(CHART_TIME_FORMAT);
    }

    public static String maskUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "Người dùng";
        }
        return username.charAt(0) + "***" + (username.length() > 2
                ? username.substring(username.length() - 1)
                : "");
    }
}
