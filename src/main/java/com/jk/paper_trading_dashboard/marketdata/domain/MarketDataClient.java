package com.jk.paper_trading_dashboard.marketdata.domain;

public interface MarketDataClient {
  MarketPrice getLatestPrice(String symbol);
}
