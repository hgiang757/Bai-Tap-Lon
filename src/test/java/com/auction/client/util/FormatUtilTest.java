package com.auction.client.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FormatUtilTest {
    @Test
    void walletLongAmountsKeepEveryDigit() {
        assertEquals("9.123.456.789.012.345 ₫", FormatUtil.formatCurrency(9_123_456_789_012_345L));
        assertEquals("9.223.372.036.854.775.807 ₫", FormatUtil.formatCurrency(Long.MAX_VALUE));
        assertEquals("100.000.000 ₫", FormatUtil.formatCurrency(100_000_000L));
    }
}
