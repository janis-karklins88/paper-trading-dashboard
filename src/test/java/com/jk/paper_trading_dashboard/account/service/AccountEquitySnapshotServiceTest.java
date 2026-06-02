package com.jk.paper_trading_dashboard.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jk.paper_trading_dashboard.account.domain.AccountEquitySnapshot;
import com.jk.paper_trading_dashboard.account.domain.AccountEquityTimeframe;
import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.dto.TradingAccountResponse;
import com.jk.paper_trading_dashboard.account.repository.AccountEquitySnapshotRepository;
import com.jk.paper_trading_dashboard.user.domain.User;

@ExtendWith(MockitoExtension.class)
class AccountEquitySnapshotServiceTest {

  private static final Instant NOW = Instant.parse("2026-06-02T08:00:00Z");

  @Mock
  private TradingAccountValuationService tradingAccountValuationService;

  @Mock
  private AccountEquitySnapshotRepository accountEquitySnapshotRepository;

  private AccountEquitySnapshotService service;
  private TradingAccount account;

  @BeforeEach
  void setUp() {
    service = new AccountEquitySnapshotService(
        tradingAccountValuationService,
        accountEquitySnapshotRepository,
        Clock.fixed(NOW, ZoneOffset.UTC));
    account = new TradingAccount(new User("test@example.com", "hash"));
  }

  @Test
  void getEquityCurveUsesTimeBoundQueryForFiniteTimeframe() {
    AccountEquitySnapshot snapshot = snapshot("100100", "2026-06-02T07:00:00Z");
    Instant from = NOW.minus(AccountEquityTimeframe.ONE_DAY.duration().orElseThrow());
    when(accountEquitySnapshotRepository
        .findByAccountIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(account.getId(), from))
        .thenReturn(List.of(snapshot));

    var response = service.getEquityCurve(account, AccountEquityTimeframe.ONE_DAY);

    assertThat(response).hasSize(1);
    assertThat(response.getFirst().equity()).isEqualByComparingTo("100100");
    assertThat(response.getFirst().timestamp()).isEqualTo(Instant.parse("2026-06-02T07:00:00Z"));
  }

  @Test
  void getEquityCurveUsesAllSnapshotsQueryForAllTimeframe() {
    AccountEquitySnapshot snapshot = snapshot("100000", "2026-06-01T08:00:00Z");
    when(accountEquitySnapshotRepository.findByAccountIdOrderByCreatedAtAsc(account.getId()))
        .thenReturn(List.of(snapshot));

    var response = service.getEquityCurve(account, AccountEquityTimeframe.ALL);

    assertThat(response).hasSize(1);
    assertThat(response.getFirst().equity()).isEqualByComparingTo("100000");
    verify(accountEquitySnapshotRepository).findByAccountIdOrderByCreatedAtAsc(account.getId());
  }

  private AccountEquitySnapshot snapshot(String equity, String createdAt) {
    AccountEquitySnapshot snapshot = new AccountEquitySnapshot(
        account,
        new TradingAccountResponse(
            new BigDecimal("100000"),
            BigDecimal.ZERO,
            new BigDecimal(equity),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            new BigDecimal("5"),
            new BigDecimal("500000")));
    snapshot.setCreatedAt(Instant.parse(createdAt));
    return snapshot;
  }
}
