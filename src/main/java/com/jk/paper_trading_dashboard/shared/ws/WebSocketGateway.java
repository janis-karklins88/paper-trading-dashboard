package com.jk.paper_trading_dashboard.shared.ws;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebSocketGateway {

  private final SimpMessagingTemplate messagingTemplate;

  public void publish(String destination, Object payload) {
    messagingTemplate.convertAndSend(destination, payload);
  }
}