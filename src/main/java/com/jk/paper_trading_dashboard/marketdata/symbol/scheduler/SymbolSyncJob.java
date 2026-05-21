package com.jk.paper_trading_dashboard.marketdata.symbol.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jk.paper_trading_dashboard.marketdata.symbol.service.SymbolSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SymbolSyncJob {

  private final SymbolSyncService symbolSyncService;

  @Scheduled(cron = "${app.symbols.sync-cron:0 0 2 * * *}")
  public void syncSymbols() {
    try {
      symbolSyncService.syncSymbols();
    } catch (RuntimeException exception) {
      log.warn("Scheduled symbol sync failed", exception);
    }
  }
}
