package com.jk.paper_trading_dashboard.account.dto;

import java.math.BigDecimal;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;

public record TradingAccountResponse(
    BigDecimal cashBalance,
    BigDecimal reservedMargin,
    BigDecimal equity,
    BigDecimal unrealizedPnl,
    BigDecimal realizedPnl,
    BigDecimal maxLeverage) {

  public static TradingAccountResponse from(TradingAccount account) {
    return new TradingAccountResponse(
        account.getCashBalance(),
        account.getReservedMargin(),
        account.getEquity(),
        account.getUnrealizedPnl(),
        account.getRealizedPnl(),
        account.getMaxLeverage());
  }
}
