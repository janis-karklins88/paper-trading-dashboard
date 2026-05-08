package com.jk.paper_trading_dashboard.alpaca.dto;

import java.math.BigDecimal;

public record AlpacaPlaceOrderRequest(
    String symbol,
    BigDecimal qty,
    String side,
    String type,
    String time_in_force,
    BigDecimal limit_price) {

}
