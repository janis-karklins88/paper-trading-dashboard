package com.jk.paper_trading_dashboard.marketdata.ws;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.jk.paper_trading_dashboard.shared.ws.WebSocketGateway;
import com.jk.paper_trading_dashboard.shared.ws.WebSocketTopics;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MarketDataPublisher {
  private final WebSocketGateway webSocketGateway;

  public void publishPriceUpdate(String symbol, BigDecimal price) {
    var message = new PriceUpdateMessage(symbol, price, Instant.now());
    webSocketGateway.publish(WebSocketTopics.price(symbol), message);
  }

}
