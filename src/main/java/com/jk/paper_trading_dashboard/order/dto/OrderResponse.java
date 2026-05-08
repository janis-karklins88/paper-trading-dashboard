package com.jk.paper_trading_dashboard.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderSide;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.domain.OrderType;

public record OrderResponse(
    UUID id,
    UUID userId,
    String symbol,
    OrderSide side,
    OrderType type,
    BigDecimal quantity,
    BigDecimal limitPrice,
    OrderStatus status,
    String brokerOrderId,
    String brokerOrderStatus,
    BigDecimal filledQuantity,
    String rejectReason,
    Instant createdAt,
    Instant submittedAt,
    Instant filledAt,
    Instant updatedAt) {

  public static OrderResponse from(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getUserId(),
        order.getSymbol(),
        order.getSide(),
        order.getType(),
        order.getQuantity(),
        order.getLimitPrice(),
        order.getStatus(),
        order.getBrokerOrderId(),
        order.getBrokerOrderStatus(),
        order.getFilledQuantity(),
        order.getRejectReason(),
        order.getCreatedAt(),
        order.getSubmittedAt(),
        order.getFilledAt(),
        order.getUpdatedAt());
  }
}
