package com.jk.paper_trading_dashboard.marketdata.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.marketdata.domain.MarketDataClient;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketPriceService {

  private final MarketDataClient marketDataClient;
  private final PriceCacheService priceCacheService;

  public Optional<MarketPrice> getCachedPrice(String symbol) {
    return priceCacheService.get(symbol);
  }

  public MarketPrice getPriceOrRefresh(String symbol) {
    return getCachedPrice(symbol).orElseGet(() -> refreshPrice(symbol));
  }

  public MarketPrice refreshPrice(String symbol) {
    MarketPrice marketPrice = marketDataClient.getLatestPrice(symbol);

    if (marketPrice == null || marketPrice.price() == null || marketPrice.price().signum() <= 0) {
      throw new IllegalStateException("Market price is unavailable for symbol " + symbol);
    }

    priceCacheService.put(marketPrice);
    return marketPrice;
  }
}
