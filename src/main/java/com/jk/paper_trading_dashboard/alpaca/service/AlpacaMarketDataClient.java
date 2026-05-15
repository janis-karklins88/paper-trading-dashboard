package com.jk.paper_trading_dashboard.alpaca.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.jk.paper_trading_dashboard.alpaca.config.AlpacaProperties;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketDataClient;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.domain.Symbols;

@Service
public class AlpacaMarketDataClient implements MarketDataClient {

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

  private record LatestTradeResponse(
      String symbol,
      LatestTrade trade) {

  }

  private record LatestTrade(
      BigDecimal p) {

  }
}
