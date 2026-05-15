package com.jk.paper_trading_dashboard.alpaca.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.jk.paper_trading_dashboard.alpaca.config.AlpacaProperties;
import com.jk.paper_trading_dashboard.marketdata.domain.Candle;
import com.jk.paper_trading_dashboard.marketdata.domain.CandleTimeFrame;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketDataClient;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.domain.Symbols;

@Service
public class AlpacaMarketDataClient implements MarketDataClient {

  private static final int CANDLE_LIMIT = 200;

  private final RestClient restClient;

  public AlpacaMarketDataClient(RestClient.Builder restClientBuilder, AlpacaProperties alpacaProperties) {
    this.restClient = restClientBuilder
        .baseUrl(alpacaProperties.baseUrl())
        .defaultHeader("APCA-API-KEY-ID", alpacaProperties.apiKey())
        .defaultHeader("APCA-API-SECRET-KEY", alpacaProperties.secretKey())
        .build();
  }

  @Override
  public MarketPrice getLatestPrice(String symbol) {
    String normalizedSymbol = Symbols.normalize(symbol);

    LatestTradeResponse response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/v2/stocks/{symbol}/trades/latest")
            .build(normalizedSymbol))
        .retrieve()
        .body(LatestTradeResponse.class);

    if (response == null || response.trade() == null || response.trade().p() == null) {
      throw new IllegalStateException("Latest trade price is missing for symbol " + normalizedSymbol);
    }

    return new MarketPrice(normalizedSymbol, response.trade().p());
  }

  @Override
  public List<Candle> getCandles(String symbol, CandleTimeFrame timeframe) {
    String normalizedSymbol = Symbols.normalize(symbol);
    Instant end = Instant.now();
    Instant start = end.minus(defaultLookback(timeframe));

    BarsResponse response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/v2/stocks/{symbol}/bars")
            .queryParam("timeframe", timeframe.getAlpacaValue())
            .queryParam("start", start)
            .queryParam("end", end)
            .queryParam("limit", CANDLE_LIMIT)
            .queryParam("adjustment", "raw")
            .queryParam("sort", "asc")
            .build(normalizedSymbol))
        .retrieve()
        .body(BarsResponse.class);

    if (response == null || response.bars() == null) {
      return List.of();
    }

    return response.bars()
        .stream()
        .map(AlpacaBar::toCandle)
        .toList();
  }

  private Duration defaultLookback(CandleTimeFrame timeframe) {
    return switch (timeframe) {
      case ONE_MINUTE -> Duration.ofDays(1);
      case FIVE_MINUTES -> Duration.ofDays(5);
      case FIFTEEN_MINUTES -> Duration.ofDays(15);
      case ONE_HOUR -> Duration.ofDays(60);
      case ONE_DAY -> Duration.ofDays(365);
    };
  }

  private record LatestTradeResponse(
      String symbol,
      LatestTrade trade) {

  }

  private record LatestTrade(
      BigDecimal p) {

  }

  private record BarsResponse(
      List<AlpacaBar> bars) {

  }

  private record AlpacaBar(
      Instant t,
      BigDecimal o,
      BigDecimal h,
      BigDecimal l,
      BigDecimal c,
      BigDecimal v) {

    private Candle toCandle() {
      return new Candle(t, o, h, l, c, v);
    }
  }
}
