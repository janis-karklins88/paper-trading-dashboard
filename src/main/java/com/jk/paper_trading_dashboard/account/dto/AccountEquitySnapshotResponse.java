package com.jk.paper_trading_dashboard.account.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.jk.paper_trading_dashboard.account.domain.AccountEquitySnapshot;

public record AccountEquitySnapshotResponse(
    UUID id,
    Instant timestamp,
    BigDecimal equity,
    BigDecimal cashBalance,
    BigDecimal reservedMargin,
    BigDecimal realizedPnl,
    BigDecimal unrealizedPnl) {

  public static AccountEquitySnapshotResponse from(AccountEquitySnapshot snapshot) {
    return new AccountEquitySnapshotResponse(
        snapshot.getId(),
        snapshot.getCreatedAt(),
        snapshot.getTotalEquity(),
        snapshot.getCashBalance(),
        snapshot.getReservedMargin(),
        snapshot.getRealizedPnl(),
        snapshot.getUnrealizedPnl());
  }
}
