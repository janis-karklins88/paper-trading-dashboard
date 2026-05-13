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
    Instant openedAt,
    Instant closedAt,
    Instant updatedAt) {

  public static PositionResponse from(Position position) {
    return new PositionResponse(
        position.getId(),
        position.getSymbol(),
        position.getSide(),
        position.getStatus(),
        position.getQuantity(),
        position.getAvgEntryPrice(),
        position.getCurrentPrice(),
        position.getUnrealizedPnl(),
        position.getRealizedPnl(),
        position.getMarginUsed(),
        position.getLeverage(),
        position.getOpenedAt(),
        position.getClosedAt(),
        position.getUpdatedAt());
  }

}
