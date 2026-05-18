package com.jk.paper_trading_dashboard.marketdata.domain;

public record DefaultMarketSymbol(
    int rank,
    String symbol,
    String name,
    String quoteSymbol) {

}
