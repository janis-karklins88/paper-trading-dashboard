package com.jk.paper_trading_dashboard.marketdata.ws;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceUpdateMessage(
    String symbol,
    BigDecimal price,
    Instant timestamp) {

}
