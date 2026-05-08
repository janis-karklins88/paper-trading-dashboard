package com.jk.paper_trading_dashboard.broker.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jk.paper_trading_dashboard.broker.domain.BrokerAccount;

public interface BrokerAccountRepository extends JpaRepository<BrokerAccount, UUID> {

  Optional<BrokerAccount> findByUserIdAndActiveTrue(UUID userId);
}
