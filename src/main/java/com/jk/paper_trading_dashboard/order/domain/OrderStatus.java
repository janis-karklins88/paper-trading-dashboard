package com.jk.paper_trading_dashboard.order.domain;

public enum OrderStatus {
  PENDING,
  SUBMITTED,
  ACCEPTED,
  PARTIALLY_FILLED,
  FILLED,
  REJECTED,
  CANCELED,
  FAILED
}
