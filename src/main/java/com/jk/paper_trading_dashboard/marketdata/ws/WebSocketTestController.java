package com.jk.paper_trading_dashboard.marketdata.ws;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jk.paper_trading_dashboard.shared.ws.WebSocketGateway;
import com.jk.paper_trading_dashboard.shared.ws.WebSocketTopics;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test/websocket")
public class WebSocketTestController {

  private final WebSocketGateway webSocketGateway;

  @PostMapping("/price")
  public void sendPriceUpdate() {

    PriceUpdateMessage message = new PriceUpdateMessage(
        "BTCUSD",
        new BigDecimal("104523.12"));

    webSocketGateway.publish(
        WebSocketTopics.price("BTCUSD"),
        message);
  }
}