package com.jk.paper_trading_dashboard.marketdata.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;

@Service
public class PriceCacheService {

  private final Map<String, MarketPrice> priceCache = new ConcurrentHashMap<>();

  public Optional<MarketPrice> get(String symbol) {
    return Optional.ofNullable(priceCache.get(normalizeSymbol(symbol)));
  }

  public void put(MarketPrice marketPrice) {
    priceCache.put(normalizeSymbol(marketPrice.symbol()), marketPrice);
  }

  public void putAll(Iterable<MarketPrice> marketPrices) {
    marketPrices.forEach(this::put);
  }

  private String normalizeSymbol(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("symbol is required");
    }

    return symbol.trim().toUpperCase();
  }
}
