package com.jk.paper_trading_dashboard.marketdata.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record MarketPrice(
    String symbol,
    BigDecimal price,
    Instant updatedAt) {

  public MarketPrice(String symbol, BigDecimal price) {
    this(symbol, price, Instant.now());
  }

  public MarketPrice {
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("symbol is required");
    }

    Objects.requireNonNull(price, "price is required");
    symbol = symbol.trim().toUpperCase();
    updatedAt = updatedAt == null ? Instant.now() : updatedAt;
  }

}
