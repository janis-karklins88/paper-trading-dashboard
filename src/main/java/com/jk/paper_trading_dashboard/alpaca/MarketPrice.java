package com.jk.paper_trading_dashboard.alpaca;

import java.math.BigDecimal;

public record MarketPrice(
    String symbol,
    BigDecimal price) {

}
