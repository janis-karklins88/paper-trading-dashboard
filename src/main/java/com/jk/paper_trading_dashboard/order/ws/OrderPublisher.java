package com.jk.paper_trading_dashboard.order.ws;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.jk.paper_trading_dashboard.order.dto.OrderResponse;
import com.jk.paper_trading_dashboard.shared.ws.WebSocketGateway;
import com.jk.paper_trading_dashboard.shared.ws.WebSocketTopics;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderPublisher {

  private final WebSocketGateway webSocketGateway;

  public void publishOrderUpdate(UUID userId, OrderResponse order) {
    webSocketGateway.publish(
        WebSocketTopics.orders(userId),
        OrderUpdateMessage.from(order));
  }
}
