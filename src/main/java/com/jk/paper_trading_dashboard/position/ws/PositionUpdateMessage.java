package com.jk.paper_trading_dashboard.position.ws;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;

public record PositionUpdateMessage(
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
    Instant updatedAt,
    Instant timestamp) {

  public static PositionUpdateMessage from(PositionResponse position) {
    return new PositionUpdateMessage(
        position.id(),
        position.symbol(),
        position.side(),
        position.status(),
        position.quantity(),
        position.avgEntryPrice(),
        position.currentPrice(),
        position.unrealizedPnl(),
        position.realizedPnl(),
        position.marginUsed(),
        position.leverage(),
        position.openedAt(),
        position.closedAt(),
        position.updatedAt(),
        Instant.now());
  }
}