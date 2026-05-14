package com.jk.paper_trading_dashboard.alpaca.domain;

import java.math.BigDecimal;

public record MarketPrice(
        String symbol,
        BigDecimal price) {

}
