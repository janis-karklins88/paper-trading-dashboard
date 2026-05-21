package com.jk.paper_trading_dashboard.marketdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jk.paper_trading_dashboard.marketdata.domain.MarketDataClient;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.ws.MarketDataPublisher;

@ExtendWith(MockitoExtension.class)
class MarketPriceServiceTest {

  @Mock
  private MarketDataClient marketDataClient;

  @Mock
  private PriceCacheService priceCacheService;

  @Mock
  private MarketDataPublisher marketDataPublisher;

  private MarketPriceService marketPriceService;

  @BeforeEach
  void setUp() {
    marketPriceService = new MarketPriceService(marketDataClient, priceCacheService, marketDataPublisher);
  }

  @Test
  void getPricesOrRefreshDeduplicatesSymbolsAndUsesCacheFirst() {
    MarketPrice cachedBtcPrice = new MarketPrice("BTC/USD", new BigDecimal("100000"));
    MarketPrice fetchedAaplPrice = new MarketPrice("AAPL", new BigDecimal("200"));
    when(priceCacheService.get("BTC/USD")).thenReturn(Optional.of(cachedBtcPrice));
    when(priceCacheService.get("AAPL")).thenReturn(Optional.empty());
    when(marketDataClient.getLatestPrice("AAPL")).thenReturn(fetchedAaplPrice);

    List<MarketPrice> prices = marketPriceService.getPricesOrRefresh(List.of("btc/usd", "AAPL", "BTC/USD"));

    assertThat(prices)
        .extracting(MarketPrice::symbol)
        .containsExactly("BTC/USD", "AAPL");
    verify(marketDataClient, never()).getLatestPrice("BTC/USD");
    verify(priceCacheService).put(fetchedAaplPrice);
    verify(marketDataPublisher).publishPriceUpdate("AAPL", new BigDecimal("200"));
  }

  @Test
  void getPricesOrRefreshSkipsUnavailableSymbols() {
    MarketPrice fetchedAaplPrice = new MarketPrice("AAPL", new BigDecimal("200"));
    when(priceCacheService.get("BTC/USD")).thenReturn(Optional.empty());
    when(priceCacheService.get("AAPL")).thenReturn(Optional.empty());
    when(marketDataClient.getLatestPrice("BTC/USD")).thenThrow(new IllegalStateException("unavailable"));
    when(marketDataClient.getLatestPrice("AAPL")).thenReturn(fetchedAaplPrice);

    List<MarketPrice> prices = marketPriceService.getPricesOrRefresh(List.of("BTC/USD", "AAPL"));

    assertThat(prices)
        .extracting(MarketPrice::symbol)
        .containsExactly("AAPL");
    verify(priceCacheService).put(fetchedAaplPrice);
    verify(marketDataPublisher).publishPriceUpdate("AAPL", new BigDecimal("200"));
  }
}
