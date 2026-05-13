package com.jk.paper_trading_dashboard.alpaca;

public interface MarketDataClient {
  MarketPrice getLatestPrice(String symbol);
}
