package com.jk.paper_trading_dashboard.watchlist.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jk.paper_trading_dashboard.watchlist.domain.Watchlist;

public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {

  List<Watchlist> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

  Optional<Watchlist> findByIdAndUserId(UUID id, UUID userId);

  boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
}
