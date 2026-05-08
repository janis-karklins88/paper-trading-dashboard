package com.jk.paper_trading_dashboard.alpaca.dto;

import java.math.BigDecimal;

public record AlpacaOrderResponse(
    String id,
    String status,
    BigDecimal filled_qty,
    BigDecimal filled_avg_price) {

}
