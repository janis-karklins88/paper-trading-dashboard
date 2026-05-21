package com.jk.paper_trading_dashboard.shared.ws;

import java.util.UUID;

public class WebSocketTopics {
  private static final String TOPIC_PREFIX = "/topic";

  private static final String PRICES = TOPIC_PREFIX + "/prices";
  private static final String CANDLES = TOPIC_PREFIX + "/candles";
  private static final String ORDERS = TOPIC_PREFIX + "/orders";
  private static final String POSITIONS = TOPIC_PREFIX + "/positions";
  private static final String PORTFOLIO = TOPIC_PREFIX + "/portfolio";
  private static final String RISK = TOPIC_PREFIX + "/risk";

  private WebSocketTopics() {
  }

  public static String price(String symbol) {
    return PRICES + "/" + normalizeSymbol(symbol);
  }

  public static String candle(String symbol, String timeframe) {
    return CANDLES + "/"
        + normalizeSymbol(symbol)
        + "/"
        + timeframe.toLowerCase();
  }

  public static String orders(UUID userId) {
    return ORDERS + "/" + userId;
  }

  public static String positions(UUID userId) {
    return POSITIONS + "/" + userId;
  }

  public static String portfolio(UUID userId) {
    return PORTFOLIO + "/" + userId;
  }

  public static String risk(UUID userId) {
    return RISK + "/" + userId;
  }

  private static String normalizeSymbol(String symbol) {
    return symbol.trim().toUpperCase();
  }
}
