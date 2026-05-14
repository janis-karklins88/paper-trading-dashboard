package com.jk.paper_trading_dashboard.alpaca.service;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.alpaca.domain.MarketDataClient;
import com.jk.paper_trading_dashboard.alpaca.domain.MarketPrice;

@Service
public class AlpacaMarketDataClient implements MarketDataClient {

  @Override
  public MarketPrice getLatestPrice(String symbol) {
    throw new UnsupportedOperationException("Alpaca market data client is not implemented yet");
  }
  
}
