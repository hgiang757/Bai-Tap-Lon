package com.auction.client.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Các phép tính thời gian dùng trong biểu mẫu phiên đấu giá. */
public final class AuctionTime {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private AuctionTime() { }
    public static LocalTime parse(String text) {
        try { return LocalTime.parse(text, TIME); } catch (DateTimeParseException | NullPointerException ex) { return null; }
    }
    public static boolean valid(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        return start != null && end != null && !start.isBefore(now) && end.isAfter(start);
    }
    public static String summary(LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        long days = minutes / 1440, hours = minutes % 1440 / 60, remaining = minutes % 60;
        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append(" ngày ");
        if (hours > 0) result.append(hours).append(" giờ ");
        if (remaining > 0 || result.isEmpty()) result.append(remaining).append(" phút ");
        return "Phiên kéo dài " + result.toString().trim() + ", kết thúc lúc " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ".";
    }
}
