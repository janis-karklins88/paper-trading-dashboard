package com.jk.paper_trading_dashboard.marketdata.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jk.paper_trading_dashboard.marketdata.service.MarketPriceUpdater;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MarketPriceUpdateJob {
  private final MarketPriceUpdater marketPriceUpdater;

  @Scheduled(fixedDelayString = "${app.market-data.refresh-delay-ms:3000}")
  public void updateMarketPrices() {
    marketPriceUpdater.updateMarketPrices();
  }
}
