package com.jk.paper_trading_dashboard.watchlist.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jk.paper_trading_dashboard.watchlist.domain.WatchlistItem;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, UUID> {

  List<WatchlistItem> findAllByWatchlistIdOrderBySortOrderAscCreatedAtAsc(UUID watchlistId);

  Optional<WatchlistItem> findByWatchlistIdAndSymbol(UUID watchlistId, String symbol);

  boolean existsByWatchlistIdAndSymbol(UUID watchlistId, String symbol);
}
