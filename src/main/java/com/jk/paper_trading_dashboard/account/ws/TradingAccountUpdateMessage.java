package com.jk.paper_trading_dashboard.account.ws;

import java.math.BigDecimal;
import java.time.Instant;

import com.jk.paper_trading_dashboard.account.dto.TradingAccountResponse;

public record TradingAccountUpdateMessage(
    BigDecimal cashBalance,
    BigDecimal reservedMargin,
    BigDecimal equity,
    BigDecimal unrealizedPnl,
    BigDecimal realizedPnl,
    BigDecimal maxLeverage,
    BigDecimal buyingPower,
    Instant timestamp) {

  public static TradingAccountUpdateMessage from(TradingAccountResponse account) {
    return new TradingAccountUpdateMessage(
        account.cashBalance(),
        account.reservedMargin(),
        account.equity(),
        account.unrealizedPnl(),
        account.realizedPnl(),
        account.maxLeverage(),
        account.buyingPower(),
        Instant.now());
  }
}
