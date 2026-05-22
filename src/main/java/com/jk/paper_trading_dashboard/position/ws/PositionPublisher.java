package com.jk.paper_trading_dashboard.position.ws;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.shared.ws.WebSocketGateway;
import com.jk.paper_trading_dashboard.shared.ws.WebSocketTopics;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PositionPublisher {
  private final WebSocketGateway webSocketGateway;

  public void publishPositionUpdate(UUID userId, PositionResponse position) {
    webSocketGateway.publish(
        WebSocketTopics.positions(userId),
        PositionUpdateMessage.from(position));
  }

}
