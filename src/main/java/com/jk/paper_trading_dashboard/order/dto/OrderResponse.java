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
    Instant updatedAt) {

  public static OrderResponse from(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getSymbol(),
        order.getSide(),
        order.getType(),
        order.getStatus(),
        order.getQuantity(),
        order.getMarginAmount(),
        order.getLeverage(),
        order.getNotionalValue(),
        order.getFeeAmount(),
        order.getFilledPrice(),
        order.getLimitPrice(),
        order.getTakeProfitPrice(),
        order.getStopLossPrice(),
        order.getRejectReason(),
        order.getCreatedAt(),
        order.getOpenedAt(),
        order.getFilledAt(),
        order.getUpdatedAt());
  }
}
