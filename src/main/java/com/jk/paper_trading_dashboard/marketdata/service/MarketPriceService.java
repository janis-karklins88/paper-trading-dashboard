package com.jk.paper_trading_dashboard.marketdata.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.marketdata.domain.Candle;
import com.jk.paper_trading_dashboard.marketdata.domain.CandleTimeFrame;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketDataClient;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.domain.Symbols;
import com.jk.paper_trading_dashboard.marketdata.ws.MarketDataPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketPriceService {

  private final MarketDataClient marketDataClient;
  private final PriceCacheService priceCacheService;
  private final MarketDataPublisher marketDataPublisher;

  public Optional<MarketPrice> getCachedPrice(String symbol) {
    return priceCacheService.get(symbol);
  }

  public MarketPrice getPriceOrRefresh(String symbol) {
    return getCachedPrice(symbol).orElseGet(() -> refreshPrice(symbol));
  }

  public List<MarketPrice> getPricesOrRefresh(List<String> symbols) {
    if (symbols == null || symbols.isEmpty()) {
      throw new IllegalArgumentException("symbols are required");
    }

    Set<String> uniqueSymbols = new LinkedHashSet<>();
    for (String symbol : symbols) {
      uniqueSymbols.add(Symbols.normalize(symbol));
    }

    return uniqueSymbols.stream()
        .map(this::getPriceOrRefreshOrSkip)
        .flatMap(Optional::stream)
        .toList();
  }

  private Optional<MarketPrice> getPriceOrRefreshOrSkip(String symbol) {
    try {
      return Optional.of(getPriceOrRefresh(symbol));
    } catch (RuntimeException exception) {
      log.warn("Skipping unavailable market price for symbol {}", symbol, exception);
      return Optional.empty();
    }
  }

  public MarketPrice refreshPrice(String symbol) {
    MarketPrice marketPrice = marketDataClient.getLatestPrice(symbol);

    if (marketPrice == null || marketPrice.price() == null || marketPrice.price().signum() <= 0) {
      throw new IllegalStateException("Market price is unavailable for symbol " + symbol);
    }

    priceCacheService.put(marketPrice);
    marketDataPublisher.publishPriceUpdate(marketPrice.symbol(), marketPrice.price());
    return marketPrice;
  }

  public List<Candle> getCandles(String symbol, CandleTimeFrame timeframe) {
    return marketDataClient.getCandles(symbol, timeframe);
  }
}
