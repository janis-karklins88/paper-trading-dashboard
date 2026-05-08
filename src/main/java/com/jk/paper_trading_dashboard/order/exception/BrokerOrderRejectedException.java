package com.jk.paper_trading_dashboard.order.exception;

public class BrokerOrderRejectedException extends RuntimeException {

  private final String brokerStatus;

  public BrokerOrderRejectedException(String message, String brokerStatus) {
    super(message);
    this.brokerStatus = brokerStatus;
  }

  public String getBrokerStatus() {
    return brokerStatus;
  }
}
