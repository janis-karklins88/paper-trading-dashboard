package com.jk.paper_trading_dashboard.marketdata.symbol.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(
    name = "app.symbols.startup-sync-enabled",
    havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SymbolStartupSyncRunner implements ApplicationRunner {

  private final SymbolSyncService symbolSyncService;

  @Override
  public void run(ApplicationArguments args) {
    try {
      symbolSyncService.syncSymbols();
    } catch (RuntimeException exception) {
      log.warn("Startup symbol sync failed; application will continue", exception);
    }
  }
}
