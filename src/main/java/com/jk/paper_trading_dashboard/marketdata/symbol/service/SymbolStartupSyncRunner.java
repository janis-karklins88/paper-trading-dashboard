package com.jk.paper_trading_dashboard.marketdata.symbol.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SymbolStartupSyncRunner implements ApplicationRunner {

  private final SymbolSyncService symbolSyncService;

  @Override
  public void run(ApplicationArguments args) {
    symbolSyncService.syncSymbols();
  }
}
