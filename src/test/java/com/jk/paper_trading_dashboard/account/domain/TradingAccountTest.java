package com.jk.paper_trading_dashboard.account.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.jk.paper_trading_dashboard.user.domain.User;

class TradingAccountTest {

  @Test
  void newAccountUsesDefaultBalances() {
    TradingAccount account = new TradingAccount(new User("test@example.com", "hash"));

    assertThat(account.getStartingCash()).isEqualByComparingTo("100000");
    assertThat(account.getCashBalance()).isEqualByComparingTo("100000");
    assertThat(account.getReservedMargin()).isEqualByComparingTo("0");
    assertThat(account.getRealizedPnl()).isEqualByComparingTo("0");
    assertThat(account.getUnrealizedPnl()).isEqualByComparingTo("0");
    assertThat(account.getMaxLeverage()).isEqualByComparingTo("5");
    assertThat(account.getStatus()).isEqualTo(TradingAccountStatus.ACTIVE);
  }

  @Test
  void reserveReleaseAndDeductFeeUpdatesBalances() {
    TradingAccount account = new TradingAccount(new User("test@example.com", "hash"));

    account.reserveMargin(new BigDecimal("1000"));
    account.deductFee(new BigDecimal("2.50"));
    account.releaseMargin(new BigDecimal("1000"));

    assertThat(account.getCashBalance()).isEqualByComparingTo("99997.50");
    assertThat(account.getReservedMargin()).isEqualByComparingTo("0");
  }

  @Test
  void resetRestoresDefaultCashAndClearsPnl() {
    TradingAccount account = new TradingAccount(new User("test@example.com", "hash"));
    account.reserveMargin(new BigDecimal("1000"));
    account.applyPositionOpen(new BigDecimal("-10"));
    account.applyPositionClose(new BigDecimal("20"), new BigDecimal("1000"), new BigDecimal("-10"));

    account.reset();

    assertThat(account.getCashBalance()).isEqualByComparingTo("100000");
    assertThat(account.getReservedMargin()).isEqualByComparingTo("0");
    assertThat(account.getRealizedPnl()).isEqualByComparingTo("0");
    assertThat(account.getUnrealizedPnl()).isEqualByComparingTo("0");
  }
}
