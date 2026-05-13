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
    UUID tradingAccountId,
    String symbol,
    OrderSide side,
    OrderType type,
    BigDecimal quantity,
    BigDecimal marginAmount,
    BigDecimal leverage,
    BigDecimal notionalValue,
    BigDecimal filledPrice,
    BigDecimal limitPrice,
    BigDecimal takeProfitPrice,
    BigDecimal stopLossPrice,
    OrderStatus status,
    String rejectReason,
    Instant createdAt,
    Instant openedAt,
    Instant filledAt,
    Instant updatedAt) {

  public static OrderResponse from(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getTradingAccountId(),
        order.getSymbol(),
        order.getSide(),
        order.getType(),
        order.getQuantity(),
        order.getMarginAmount(),
        order.getLeverage(),
        order.getNotionalValue(),
        order.getFilledPrice(),
        order.getLimitPrice(),
        order.getTakeProfitPrice(),
        order.getStopLossPrice(),
        order.getStatus(),
        order.getRejectReason(),
        order.getCreatedAt(),
        order.getOpenedAt(),
        order.getFilledAt(),
        order.getUpdatedAt());
  }
}
