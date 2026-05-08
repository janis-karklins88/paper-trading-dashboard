package com.jk.paper_trading_dashboard.alpaca.dto;

import java.math.BigDecimal;

import com.jk.paper_trading_dashboard.order.domain.Order;

public record BrokerOrderRequest(
    String symbol,
    BigDecimal quantity,
    String side,
    String type,
    BigDecimal limitPrice) {

  public static BrokerOrderRequest from(Order order) {
    return new BrokerOrderRequest(
        order.getSymbol(),
        order.getQuantity(),
        order.getSide().name().toLowerCase(),
        order.getType().name().toLowerCase(),
        order.getLimitPrice());
  }
}
