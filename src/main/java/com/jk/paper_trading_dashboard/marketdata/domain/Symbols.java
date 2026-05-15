package com.jk.paper_trading_dashboard.marketdata.domain;

public final class Symbols {

  private Symbols() {

  }

  public static String normalize(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("symbol is required");
    }

    return symbol.trim().toUpperCase();
  }
}
