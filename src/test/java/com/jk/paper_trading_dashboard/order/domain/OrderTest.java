package com.jk.paper_trading_dashboard.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OrderTest {

  @Test
  void marketOrderCalculatesNotionalAndQuantityWhenFilled() {
    Order order = Order.pendingMarketOrder(
        UUID.randomUUID(),
        "tsla",
        OrderSide.BUY,
        new BigDecimal("1000"),
        new BigDecimal("5"),
        new BigDecimal("300"),
        new BigDecimal("220"));

    order.markFilled(new BigDecimal("250"));

    assertThat(order.getSymbol()).isEqualTo("TSLA");
    assertThat(order.getNotionalValue()).isEqualByComparingTo("5000");
    assertThat(order.getQuantity()).isEqualByComparingTo("20.00000000");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
    assertThat(order.getFilledPrice()).isEqualByComparingTo("250");
  }

  @Test
  void limitOrderCanBeMarkedOpenAndStoresFee() {
    Order order = Order.pendingLimitOrder(
        UUID.randomUUID(),
        "AAPL",
        OrderSide.SELL,
        new BigDecimal("500"),
        new BigDecimal("3"),
        new BigDecimal("190"),
        null,
        null);

    order.applyFee(new BigDecimal("0.15000000"));
    order.markOpen();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
    assertThat(order.getFeeAmount()).isEqualByComparingTo("0.15000000");
    assertThat(order.getOpenedAt()).isNotNull();
  }

  @Test
  void filledOrderCannotBeCanceled() {
    Order order = Order.pendingMarketOrder(
        UUID.randomUUID(),
        "TSLA",
        OrderSide.BUY,
        new BigDecimal("1000"),
        new BigDecimal("5"),
        null,
        null);
    order.markFilled(new BigDecimal("250"));

    assertThatThrownBy(order::markCanceled)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Filled or rejected orders cannot be canceled");
  }
}
