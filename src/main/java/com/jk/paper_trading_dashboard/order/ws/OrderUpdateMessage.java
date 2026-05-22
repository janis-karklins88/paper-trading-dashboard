package com.jk.paper_trading_dashboard.order.ws;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.jk.paper_trading_dashboard.order.domain.OrderSide;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.domain.OrderType;
import com.jk.paper_trading_dashboard.order.dto.OrderResponse;

public record OrderUpdateMessage(
    UUID id,
    String symbol,
    OrderSide side,
    OrderType type,
    OrderStatus status,
    BigDecimal quantity,
    BigDecimal marginAmount,
    BigDecimal leverage,
    BigDecimal notionalValue,
    BigDecimal feeAmount,
    BigDecimal executionPrice,
    BigDecimal limitPrice,
    BigDecimal takeProfitPrice,
    BigDecimal stopLossPrice,
    String rejectReason,
    Instant createdAt,
    Instant openedAt,
    Instant filledAt,
    Instant updatedAt,
    Instant timestamp) {

  public static OrderUpdateMessage from(OrderResponse order) {
    return new OrderUpdateMessage(
        order.id(),
        order.symbol(),
        order.side(),
        order.type(),
        order.status(),
        order.quantity(),
        order.marginAmount(),
        order.leverage(),
        order.notionalValue(),
        order.feeAmount(),
        order.executionPrice(),
        order.limitPrice(),
        order.takeProfitPrice(),
        order.stopLossPrice(),
        order.rejectReason(),
        order.createdAt(),
        order.openedAt(),
        order.filledAt(),
        order.updatedAt(),
        Instant.now());
  }
}
