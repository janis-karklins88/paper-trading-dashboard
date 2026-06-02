package com.jk.paper_trading_dashboard.account.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jk.paper_trading_dashboard.account.domain.AccountEquitySnapshot;

public interface AccountEquitySnapshotRepository extends JpaRepository<AccountEquitySnapshot, UUID> {

  List<AccountEquitySnapshot> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

  List<AccountEquitySnapshot> findByAccountIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
      UUID accountId,
      Instant createdAt);
}
