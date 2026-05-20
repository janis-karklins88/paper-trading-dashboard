package com.jk.paper_trading_dashboard.watchlist.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jk.paper_trading_dashboard.watchlist.domain.Watchlist;

public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {

  List<Watchlist> findAllByUser_IdOrderByCreatedAtAsc(UUID userId);

  Optional<Watchlist> findByIdAndUser_Id(UUID id, UUID userId);

  Optional<Watchlist> findByUser_IdAndNameIgnoreCase(UUID userId, String name);

  boolean existsByUser_IdAndNameIgnoreCase(UUID userId, String name);
}
