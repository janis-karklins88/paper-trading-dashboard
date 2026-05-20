package com.jk.paper_trading_dashboard.watchlist.config;

import java.util.List;

public record DefaultWatchlistConfig(
    String name,
    List<String> symbols) {

}
