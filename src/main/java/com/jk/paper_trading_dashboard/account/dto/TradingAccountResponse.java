package com.jk.paper_trading_dashboard.account.dto;

import java.math.BigDecimal;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;

public record TradingAccountResponse(
    BigDecimal cashBalance,
    BigDecimal reservedMargin,
    BigDecimal equity,
    BigDecimal unrealizedPnl,
    BigDecimal realizedPnl,
    BigDecimal netPnl,
    BigDecimal maxLeverage,
    BigDecimal buyingPower) {

  public static TradingAccountResponse from(TradingAccount account) {
    return from(account, account.getUnrealizedPnl());
  }

  public static TradingAccountResponse from(TradingAccount account, BigDecimal unrealizedPnl) {
    BigDecimal equity = account.getCashBalance()
        .add(account.getReservedMargin())
        .add(unrealizedPnl);

    return new TradingAccountResponse(
        account.getCashBalance(),
        account.getReservedMargin(),
        equity,
        unrealizedPnl,
        account.getRealizedPnl(),
        equity.subtract(account.getStartingCash()),
        account.getMaxLeverage(),
        account.getCashBalance().multiply(account.getMaxLeverage()));
  }
}
