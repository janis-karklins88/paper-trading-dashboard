package com.jk.paper_trading_dashboard.alpaca;

import java.util.List;

import com.jk.paper_trading_dashboard.alpaca.dto.BrokerOrderRequest;
import com.jk.paper_trading_dashboard.alpaca.dto.BrokerOrderResponse;

public interface BrokerClient {
    MarketPrice getLatestPrice(String symbol);

    List<Candle> getCandles(...);
}
