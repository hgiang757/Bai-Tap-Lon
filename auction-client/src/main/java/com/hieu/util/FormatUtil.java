package com.hieu.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Tiện ích định dạng dữ liệu hiển thị chuẩn tiếng Việt.
 */
public final class FormatUtil {

    private static final DecimalFormat CURRENCY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        CURRENCY_FORMAT = new DecimalFormat("#,##0", symbols);
    }

    private FormatUtil() {
    }

    /**
     * Định dạng số tiền theo quy chuẩn Việt Nam: 185.000.000 ₫
     */
    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount) + " ₫";
    }
}
