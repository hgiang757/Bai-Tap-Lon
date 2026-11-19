package com.auction.server.assistant;

import java.net.URI;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;

/** Source of verified prices for comparable products. */
@FunctionalInterface
public interface MarketPriceService {
  /**
   * Returns a documented range for the same brand, model, version and condition, or empty when
   * there is insufficient evidence. Implementations must bound their lookup time and honor
   * interruption.
   */
  Optional<Quote> findComparablePrices(AuctionProduct product) throws InterruptedException;

  /** A verified range in whole currency units with its source and retrieval time. */
  record Quote(long low, long high, Currency currency, URI source, Instant retrievedAt) {
    /** Validates the range and source metadata. */
    public Quote {
      Objects.requireNonNull(currency);
      Objects.requireNonNull(source);
      Objects.requireNonNull(retrievedAt);
      if (low <= 0
          || high < low
          || source.getHost() == null
          || !"https".equalsIgnoreCase(source.getScheme())) {
        throw new IllegalArgumentException("Invalid verified market price");
      }
    }
  }
}
