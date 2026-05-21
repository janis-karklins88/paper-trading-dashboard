package com.jk.paper_trading_dashboard.marketdata.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class ActiveMarketSymbolServiceTest {

  @Test
  void getActiveSymbolsReturnsNormalizedSymbols() {
    ActiveMarketSymbolService service = new ActiveMarketSymbolService(
        Clock.fixed(Instant.parse("2026-05-21T12:00:00Z"), ZoneOffset.UTC),
        30_000);

    service.track("btc/usd");

    assertThat(service.getActiveSymbols()).containsExactly("BTC/USD");
  }

  @Test
  void getActiveSymbolsDropsExpiredSymbols() {
    MutableClock clock = new MutableClock(Instant.parse("2026-05-21T12:00:00Z"));
    ActiveMarketSymbolService service = new ActiveMarketSymbolService(clock, 30_000);

    service.track("AAPL");
    clock.setInstant(Instant.parse("2026-05-21T12:00:31Z"));

    assertThat(service.getActiveSymbols()).isEmpty();
  }

  private static class MutableClock extends Clock {

    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    private void setInstant(Instant instant) {
      this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
