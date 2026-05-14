package com.jk.paper_trading_dashboard.alpaca.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jk.paper_trading_dashboard.alpaca.domain.MarketDataClient;
import com.jk.paper_trading_dashboard.alpaca.domain.MarketPrice;

@RestController
@RequestMapping("/market-data")
public class MarketDataController {

  private final MarketDataClient marketDataClient;

  public MarketDataController(MarketDataClient marketDataClient) {
    this.marketDataClient = marketDataClient;
  }

  @GetMapping("/latest-price")
  public ResponseEntity<MarketPrice> getLatestPrice(@RequestParam String symbol) {
    return ResponseEntity.ok(marketDataClient.getLatestPrice(symbol));
  }
}
