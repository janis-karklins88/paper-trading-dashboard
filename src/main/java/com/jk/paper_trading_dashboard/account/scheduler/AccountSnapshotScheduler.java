package com.jk.paper_trading_dashboard.account.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.repository.TradingAccountRepository;
import com.jk.paper_trading_dashboard.account.service.AccountEquitySnapshotService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountSnapshotScheduler {
  private final AccountEquitySnapshotService accountEquitySnapshotService;
  private final TradingAccountRepository accountRepository;

  @Scheduled(fixedDelayString = "${app.account-snapshot.delay-ms:50000}")
  public void snapshotAccounts() {
    List<TradingAccount> accounts = accountRepository.findAccountsWithOpenPositions();
    for (TradingAccount account : accounts) {
      accountEquitySnapshotService.createSnapshotForAccount(account);
    }
  }
}
