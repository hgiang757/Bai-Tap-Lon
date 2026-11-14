package com.auction.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProductFormValuesTest {
  @Test
  void dateParsingIsStrictAndAcceptsLeapYearsWithoutChangingLocale() {
    final var locale = java.util.Locale.getDefault();
    assertEquals(LocalDate.of(2028, 2, 29), AuctionScheduleInput.parseDate("29/02/2028"));
    for (String invalid : new String[] {"29/02/2027", "31/04/2028", "1/2/2028", "2028-02-29", ""}) {
      assertNull(AuctionScheduleInput.parseDate(invalid), invalid);
    }
    assertNull(AuctionScheduleInput.parseDate(null));
    assertEquals(locale, java.util.Locale.getDefault());
  }

  @Test
  void moneyIsExactIntegralAndWithinTheExistingBackendRange() {
    assertEquals(5_000_000L, SellerProductFormView.amount("5000000.0"));
    assertEquals(9_000_000_000_000L, SellerProductFormView.amount("9000000000000"));
    for (String invalid : new String[] {"0", "-1", "1.1", "9000000000001", "NaN", "Infinity", ""}) {
      assertNull(SellerProductFormView.amount(invalid), invalid);
    }
    assertNull(SellerProductFormView.amount(null));
  }
}
