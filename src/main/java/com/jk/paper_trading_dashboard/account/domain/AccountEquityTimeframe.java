package com.jk.paper_trading_dashboard.account.domain;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import com.jk.paper_trading_dashboard.shared.exception.BadRequestException;

public enum AccountEquityTimeframe {
  ONE_DAY(Duration.ofDays(1)),
  ONE_WEEK(Duration.ofDays(7)),
  ONE_MONTH(Duration.ofDays(30)),
  ONE_YEAR(Duration.ofDays(365)),
  ALL(null);

  private final Duration duration;

  AccountEquityTimeframe(Duration duration) {
    this.duration = duration;
  }

  public Optional<Duration> duration() {
    return Optional.ofNullable(duration);
  }

  public static AccountEquityTimeframe fromValue(String value) {
    if (value == null || value.isBlank()) {
      return ALL;
    }

    return switch (value.trim().toUpperCase(Locale.ROOT)) {
      case "1D", "1DAY", "ONE_DAY" -> ONE_DAY;
      case "1W", "1WEEK", "ONE_WEEK" -> ONE_WEEK;
      case "1M", "1MONTH", "ONE_MONTH" -> ONE_MONTH;
      case "1Y", "1YEAR", "ONE_YEAR" -> ONE_YEAR;
      case "ALL" -> ALL;
      default -> throw new BadRequestException("Unsupported equity curve timeframe");
    };
  }
}
