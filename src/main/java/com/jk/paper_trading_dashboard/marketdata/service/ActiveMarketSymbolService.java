package com.jk.paper_trading_dashboard.marketdata.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.marketdata.domain.Symbols;

@Service
public class ActiveMarketSymbolService {

  private final Map<String, Instant> activeSymbols = new ConcurrentHashMap<>();
  private final Clock clock;
  private final Duration ttl;

  public ActiveMarketSymbolService(
      Clock clock,
      @Value("${app.market-data.active-symbol-ttl-ms:30000}") long ttlMs) {
    this.clock = clock;
    this.ttl = Duration.ofMillis(ttlMs);
  }

  public void track(String symbol) {
    activeSymbols.put(Symbols.normalize(symbol), clock.instant());
  }

  public List<String> getActiveSymbols() {
    Instant expiresBefore = clock.instant().minus(ttl);

    activeSymbols.entrySet().removeIf(entry -> entry.getValue().isBefore(expiresBefore));

    return List.copyOf(activeSymbols.keySet());
  }
}
