package com.jk.paper_trading_dashboard.alpaca.domain;

public interface MarketDataClient {
  MarketPrice getLatestPrice(String symbol);
}
