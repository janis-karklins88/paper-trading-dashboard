package com.jk.paper_trading_dashboard.account.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.domain.TradingAccountStatus;

public interface TradingAccountRepository extends JpaRepository<TradingAccount, UUID> {

  Optional<TradingAccount> findByUser_Id(UUID userId);

  Optional<TradingAccount> findByUser_IdAndStatus(UUID userId, TradingAccountStatus status);

  boolean existsByUser_Id(UUID userId);

  @Query("""
      select distinct a
      from TradingAccount a
      where exists (
        select 1
        from Position p
        where p.tradingAccountId = a.id
          and p.status = com.jk.paper_trading_dashboard.position.domain.PositionStatus.OPEN
      )
      """)
  List<TradingAccount> findAccountsWithOpenPositions();

}
