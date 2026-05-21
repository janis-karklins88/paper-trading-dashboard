package com.jk.paper_trading_dashboard.alpaca.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    if (isCryptoPair(normalizedSymbol)) {
      return getLatestCryptoPrice(normalizedSymbol);
    }

    LatestTradeResponse response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/v2/stocks/{symbol}/trades/latest")
            .queryParam("feed", "iex")
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

    if (isCryptoPair(normalizedSymbol)) {
      return getCryptoCandles(normalizedSymbol, timeframe);
    }

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
            .queryParam("feed", "iex")
            .queryParam("sort", "desc")
            .build(normalizedSymbol))
        .retrieve()
        .body(BarsResponse.class);

    if (response == null || response.bars() == null) {
      return List.of();
    }

    return toAscendingCandles(response.bars());
  }

  private MarketPrice getLatestCryptoPrice(String symbol) {
    CryptoLatestTradesResponse response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/v1beta3/crypto/us/latest/trades")
            .queryParam("symbols", symbol)
            .build())
        .retrieve()
        .body(CryptoLatestTradesResponse.class);

    LatestTrade trade = response == null || response.trades() == null ? null : getTradeForSymbol(response.trades(), symbol);

    if (trade == null || trade.p() == null) {
      throw new IllegalStateException("Latest crypto trade price is missing for symbol " + symbol);
    }

    return new MarketPrice(symbol, trade.p());
  }

  private List<Candle> getCryptoCandles(String symbol, CandleTimeFrame timeframe) {
    Instant end = Instant.now();
    Instant start = end.minus(defaultLookback(timeframe));

    CryptoBarsResponse response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/v1beta3/crypto/us/bars")
            .queryParam("symbols", symbol)
            .queryParam("timeframe", timeframe.getAlpacaValue())
            .queryParam("start", start)
            .queryParam("end", end)
            .queryParam("limit", CANDLE_LIMIT)
            .queryParam("sort", "desc")
            .build())
        .retrieve()
        .body(CryptoBarsResponse.class);

    List<AlpacaBar> bars = response == null || response.bars() == null ? null : getBarsForSymbol(response.bars(), symbol);

    if (bars == null) {
      return List.of();
    }

    return toAscendingCandles(bars);
  }

  private List<Candle> toAscendingCandles(List<AlpacaBar> bars) {
    return bars.reversed()
        .stream()
        .map(AlpacaBar::toCandle)
        .toList();
  }

  private List<AlpacaBar> getBarsForSymbol(Map<String, List<AlpacaBar>> barsBySymbol, String symbol) {
    List<AlpacaBar> bars = barsBySymbol.get(symbol);

    if (bars != null) {
      return bars;
    }

    String symbolWithoutSeparator = symbol.replace("/", "");
    return barsBySymbol.entrySet()
        .stream()
        .filter(entry -> entry.getKey().equalsIgnoreCase(symbol)
            || entry.getKey().replace("/", "").equalsIgnoreCase(symbolWithoutSeparator))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  private LatestTrade getTradeForSymbol(Map<String, LatestTrade> tradesBySymbol, String symbol) {
    LatestTrade trade = tradesBySymbol.get(symbol);

    if (trade != null) {
      return trade;
    }

    String symbolWithoutSeparator = symbol.replace("/", "");
    return tradesBySymbol.entrySet()
        .stream()
        .filter(entry -> entry.getKey().equalsIgnoreCase(symbol)
            || entry.getKey().replace("/", "").equalsIgnoreCase(symbolWithoutSeparator))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  private boolean isCryptoPair(String symbol) {
    return symbol.contains("/");
  }

  private Duration defaultLookback(CandleTimeFrame timeframe) {
    return switch (timeframe) {
      case ONE_MINUTE -> Duration.ofDays(5);
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

  private record CryptoLatestTradesResponse(
      Map<String, LatestTrade> trades) {

  }

  private record CryptoBarsResponse(
      Map<String, List<AlpacaBar>> bars) {

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
