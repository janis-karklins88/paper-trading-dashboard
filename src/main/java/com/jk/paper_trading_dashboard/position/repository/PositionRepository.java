package com.jk.paper_trading_dashboard.position.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;

public interface PositionRepository extends JpaRepository<Position, UUID> {

  List<Position> findByTradingAccountIdOrderByOpenedAtDesc(UUID tradingAccountId);

  List<Position> findByTradingAccountIdAndStatusOrderByOpenedAtDesc(UUID tradingAccountId, PositionStatus status);

  Optional<Position> findByIdAndTradingAccountId(UUID id, UUID tradingAccountId);

  boolean existsByTradingAccountIdAndStatus(UUID tradingAccountId, PositionStatus status);

  List<Position> findByStatus(PositionStatus open);
}
