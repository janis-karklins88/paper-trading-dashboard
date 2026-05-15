package com.jk.paper_trading_dashboard.marketdata.domain;

public enum CandleTimeFrame {

  ONE_MINUTE("1Min", "1m"),
  FIVE_MINUTES("5Min", "5m"),
  FIFTEEN_MINUTES("15Min", "15m"),
  ONE_HOUR("1Hour", "1h"),
  ONE_DAY("1Day", "1d");

  private final String alpacaValue;
  private final String frontendValue;

  CandleTimeFrame(String alpacaValue, String frontendValue) {
    this.alpacaValue = alpacaValue;
    this.frontendValue = frontendValue;
  }

  public String getAlpacaValue() {
    return alpacaValue;
  }

  public String getFrontendValue() {
    return frontendValue;
  }

  public static CandleTimeFrame fromValue(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("timeframe is required");
    }

    String normalizedValue = value.trim();

    for (CandleTimeFrame timeframe : values()) {
      if (timeframe.name().equalsIgnoreCase(normalizedValue)
          || timeframe.alpacaValue.equalsIgnoreCase(normalizedValue)
          || timeframe.frontendValue.equalsIgnoreCase(normalizedValue)) {
        return timeframe;
      }
    }

    throw new IllegalArgumentException("Unsupported candle timeframe: " + value);
  }
}
