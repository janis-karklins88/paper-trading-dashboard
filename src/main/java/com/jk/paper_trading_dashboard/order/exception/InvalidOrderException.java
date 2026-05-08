package com.jk.paper_trading_dashboard.order.exception;

import com.jk.paper_trading_dashboard.shared.exception.BadRequestException;

public class InvalidOrderException extends BadRequestException {

  public InvalidOrderException(String message) {
    super(message);
  }
}
