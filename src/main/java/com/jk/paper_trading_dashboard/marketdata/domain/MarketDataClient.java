package com.jk.paper_trading_dashboard.marketdata.domain;

import java.util.List;

public interface MarketDataClient {
  MarketPrice getLatestPrice(String symbol);

  List<Candle> getCandles(String symbol, CandleTimeFrame timeframe);
}
