package com.jk.paper_trading_dashboard.marketdata.ws;

import java.math.BigDecimal;

public record PriceUpdateMessage(
    String symbol,
    BigDecimal price) {

}
