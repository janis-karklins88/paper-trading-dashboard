package com.jk.paper_trading_dashboard.position.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;

public record PositionResponse(
    UUID id,
    UUID tradingAccountId,
    String symbol,
    PositionSide side,
    BigDecimal quantity,
    BigDecimal avgEntryPrice,
    BigDecimal currentPrice,
    BigDecimal marginUsed,
    BigDecimal leverage,
    BigDecimal unrealizedPnl,
    BigDecimal realizedPnl,
    PositionStatus status,
    Instant openedAt,
    Instant closedAt,
    Instant updatedAt) {

  public static PositionResponse from(Position position) {
    return new PositionResponse(
        position.getId(),
        position.getTradingAccountId(),
        position.getSymbol(),
        position.getSide(),
        position.getQuantity(),
        position.getAvgEntryPrice(),
        position.getCurrentPrice(),
        position.getMarginUsed(),
        position.getLeverage(),
        position.getUnrealizedPnl(),
        position.getRealizedPnl(),
        position.getStatus(),
        position.getOpenedAt(),
        position.getClosedAt(),
        position.getUpdatedAt());
  }

}
