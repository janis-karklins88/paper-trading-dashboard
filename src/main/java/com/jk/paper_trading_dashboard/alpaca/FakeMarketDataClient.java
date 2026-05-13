package com.jk.paper_trading_dashboard.alpaca;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class FakeMarketDataClient implements MarketDataClient {

  private static final Map<String, BigDecimal> PRICES = Map.of(
      "AAPL", new BigDecimal("190.00"),
      "MSFT", new BigDecimal("420.00"),
      "NVDA", new BigDecimal("900.00"),
      "SPY", new BigDecimal("500.00"),
      "TSLA", new BigDecimal("250.00"));

  @Override
  public MarketPrice getLatestPrice(String symbol) {
    String normalizedSymbol = symbol == null ? "" : symbol.trim().toUpperCase();
    BigDecimal price = PRICES.getOrDefault(normalizedSymbol, new BigDecimal("100.00"));
    return new MarketPrice(normalizedSymbol, price);
  }
}
