package com.jk.paper_trading_dashboard.marketdata.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jk.paper_trading_dashboard.marketdata.domain.Candle;
import com.jk.paper_trading_dashboard.marketdata.domain.CandleTimeFrame;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.dto.LatestPricesRequest;
import com.jk.paper_trading_dashboard.marketdata.dto.TrackActiveSymbolRequest;
import com.jk.paper_trading_dashboard.marketdata.service.ActiveMarketSymbolService;
import com.jk.paper_trading_dashboard.marketdata.service.MarketPriceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/market-data")
@RequiredArgsConstructor
public class MarketDataController {

  private final MarketPriceService marketPriceService;
  private final ActiveMarketSymbolService activeMarketSymbolService;

  @GetMapping("/latest-price")
  public ResponseEntity<MarketPrice> getLatestPrice(@RequestParam String symbol) {
    return ResponseEntity.ok(marketPriceService.getPriceOrRefresh(symbol));
  }

  @PostMapping("/latest-prices")
  public ResponseEntity<List<MarketPrice>> getLatestPrices(@Valid @RequestBody LatestPricesRequest request) {
    return ResponseEntity.ok(marketPriceService.getPricesOrRefresh(request.symbols()));
  }

  @PostMapping("/active-symbol")
  public ResponseEntity<Void> trackActiveSymbol(@Valid @RequestBody TrackActiveSymbolRequest request) {
    activeMarketSymbolService.track(request.symbol());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/candles")
  public ResponseEntity<List<Candle>> getCandles(@RequestParam String symbol, @RequestParam String timeframe) {
    return ResponseEntity.ok(marketPriceService.getCandles(symbol, CandleTimeFrame.fromValue(timeframe)));
  }
}
