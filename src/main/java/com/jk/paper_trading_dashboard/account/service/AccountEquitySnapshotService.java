package com.jk.paper_trading_dashboard.account.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.domain.AccountEquitySnapshot;
import com.jk.paper_trading_dashboard.account.domain.AccountEquityTimeframe;
import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.dto.AccountEquitySnapshotResponse;
import com.jk.paper_trading_dashboard.account.repository.AccountEquitySnapshotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountEquitySnapshotService {
  private final TradingAccountValuationService tradingAccountValuationService;

  private final AccountEquitySnapshotRepository accountEquitySnapshotRepository;
  private final Clock clock;

  public void createSnapshotForAccount(TradingAccount account) {
    var accountSummary = tradingAccountValuationService.getAccountSummary(account);
    var snapshot = new AccountEquitySnapshot(account, accountSummary);
    accountEquitySnapshotRepository.save(snapshot);
  }

  public List<AccountEquitySnapshotResponse> getEquityCurve(
      TradingAccount account,
      AccountEquityTimeframe timeframe) {
    List<AccountEquitySnapshot> snapshots = timeframe.duration()
        .map(duration -> {
          Instant from = Instant.now(clock).minus(duration);
          return accountEquitySnapshotRepository
              .findByAccountIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(account.getId(), from);
        })
        .orElseGet(() -> accountEquitySnapshotRepository.findByAccountIdOrderByCreatedAtAsc(account.getId()));

    return snapshots.stream()
        .map(AccountEquitySnapshotResponse::from)
        .toList();
  }
}
