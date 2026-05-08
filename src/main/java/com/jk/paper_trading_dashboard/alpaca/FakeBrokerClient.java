package com.jk.paper_trading_dashboard.alpaca;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.alpaca.dto.BrokerOrderRequest;
import com.jk.paper_trading_dashboard.alpaca.dto.BrokerOrderResponse;

@Service
public class FakeBrokerClient implements BrokerClient {

  private static final Logger log = LoggerFactory.getLogger(FakeBrokerClient.class);

  @Override
  public BrokerOrderResponse placeOrder(BrokerOrderRequest request) {
    return new BrokerOrderResponse(
        UUID.randomUUID().toString(),
        "accepted",
        BigDecimal.ZERO,
        null,
        null,
        Instant.now());
  }

  @Override
  public void cancelOrder(String brokerOrderId) {
    log.info("Canceled fake order {}", brokerOrderId);
  }

  @Override
  public BrokerOrderResponse getOrder(String brokerOrderId) {

    return new BrokerOrderResponse(
        brokerOrderId,
        "filled",
        new BigDecimal("10"),
        new BigDecimal("185.42"),
        null,
        Instant.now());
  }

}
