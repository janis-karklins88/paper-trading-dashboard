package com.jk.paper_trading_dashboard.marketdata.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.domain.Symbols;

@Service
public class PriceCacheService {

  private final Map<String, MarketPrice> priceCache = new ConcurrentHashMap<>();

  public Optional<MarketPrice> get(String symbol) {
    return Optional.ofNullable(priceCache.get(Symbols.normalize(symbol)));
  }

  public void put(MarketPrice marketPrice) {
    priceCache.put(Symbols.normalize(marketPrice.symbol()), marketPrice);
  }

  public void putAll(Iterable<MarketPrice> marketPrices) {
    marketPrices.forEach(this::put);
  }
}
