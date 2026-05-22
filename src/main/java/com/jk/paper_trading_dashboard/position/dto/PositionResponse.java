package com.jk.paper_trading_dashboard.position.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;

public record PositionResponse(
    UUID id,
    String symbol,
    PositionSide side,
    PositionStatus status,
    BigDecimal quantity,
    BigDecimal avgEntryPrice,
    BigDecimal currentPrice,
    BigDecimal unrealizedPnl,
    BigDecimal realizedPnl,
    BigDecimal marginUsed,
    BigDecimal leverage,
    BigDecimal takeProfitPrice,
    BigDecimal stopLossPrice,
    Instant openedAt,
    Instant closedAt,
    Instant updatedAt) {

  public static PositionResponse from(Position position) {
    return from(position, position.getCurrentPrice(), position.getUnrealizedPnl());
  }

  public static PositionResponse from(Position position, BigDecimal currentPrice, BigDecimal unrealizedPnl) {
    return new PositionResponse(
        position.getId(),
        position.getSymbol(),
        position.getSide(),
        position.getStatus(),
        position.getQuantity(),
        position.getAvgEntryPrice(),
        currentPrice,
        unrealizedPnl,
        position.getRealizedPnl(),
        position.getMarginUsed(),
        position.getLeverage(),
        position.getTakeProfitPrice(),
        position.getStopLossPrice(),
        position.getOpenedAt(),
        position.getClosedAt(),
        position.getUpdatedAt());
  }

}
