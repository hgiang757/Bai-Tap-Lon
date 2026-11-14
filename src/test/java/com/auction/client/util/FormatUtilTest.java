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

  @Test
  void concurrentCurrencyAndChartFormattingKeepsExactValues() throws Exception {
    var start = new java.util.concurrent.CountDownLatch(1);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(8)) {
      var results = new java.util.ArrayList<java.util.concurrent.Future<?>>();
      for (int worker = 0; worker < 8; worker++) {
        results.add(
            pool.submit(
                () -> {
                  start.await();
                  for (int i = 0; i < 2000; i++) {
                    assertEquals(
                        "9.123.456.789.012.345 ₫",
                        FormatUtil.formatCurrency(9_123_456_789_012_345L));
                    assertEquals("-123.456 ₫", FormatUtil.formatCurrency(-123_456L));
                    assertEquals("1.234.567 ₫", FormatUtil.formatCurrency(1_234_567.0));
                    assertEquals("2 tr", FormatUtil.formatCompactPriceAxis(2_000_000));
                  }
                  return null;
                }));
      }
      start.countDown();
      for (var result : results) {
        result.get(15, java.util.concurrent.TimeUnit.SECONDS);
      }
    }
  }
}
