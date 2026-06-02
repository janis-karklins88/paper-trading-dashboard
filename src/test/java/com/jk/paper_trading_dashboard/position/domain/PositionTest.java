package com.jk.paper_trading_dashboard.position.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PositionTest {

  @Test
  void longPositionCalculatesAndRealizesPnlOnClose() {
    Position position = new Position(
        UUID.randomUUID(),
        "tsla",
        PositionSide.LONG,
        new BigDecimal("20"),
        new BigDecimal("250"),
        new BigDecimal("255"),
        new BigDecimal("1000"),
        new BigDecimal("5"));

    BigDecimal realizedPnl = position.close();

    assertThat(realizedPnl).isEqualByComparingTo("100");
    assertThat(position.getStatus()).isEqualTo(PositionStatus.CLOSED);
    assertThat(position.getRealizedPnl()).isEqualByComparingTo("100");
    assertThat(position.getUnrealizedPnl()).isEqualByComparingTo("0");
    assertThat(position.getClosedAt()).isNotNull();
  }

  @Test
  void newPositionLeavesVersionUnsetForJpaPersist() {
    Position position = new Position(
        UUID.randomUUID(),
        "TSLA",
        PositionSide.LONG,
        new BigDecimal("20"),
        new BigDecimal("250"),
        new BigDecimal("255"),
        new BigDecimal("1000"),
        new BigDecimal("5"));

    assertThat(position.getVersion()).isNull();
  }

  @Test
  void closedPositionCannotBeClosedAgain() {
    Position position = new Position(
        UUID.randomUUID(),
        "TSLA",
        PositionSide.SHORT,
        new BigDecimal("10"),
        new BigDecimal("250"),
        new BigDecimal("240"),
        new BigDecimal("500"),
        new BigDecimal("5"));
    position.close();

    assertThatThrownBy(position::close)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Only open positions can be closed");
  }
}
