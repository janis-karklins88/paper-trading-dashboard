package com.jk.paper_trading_dashboard.watchlist.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jk.paper_trading_dashboard.watchlist.domain.WatchlistItem;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, UUID> {

  List<WatchlistItem> findAllByWatchlist_IdOrderBySortOrderAscCreatedAtAsc(UUID watchlistId);

  Optional<WatchlistItem> findByWatchlist_IdAndSymbol(UUID watchlistId, String symbol);

  Optional<WatchlistItem> findByIdAndWatchlist_Id(UUID id, UUID watchlistId);

  boolean existsByWatchlist_IdAndSymbol(UUID watchlistId, String symbol);

  @Query("SELECT COALESCE(MAX(i.sortOrder), -1) FROM WatchlistItem i WHERE i.watchlist.id = :watchlistId")
  int findMaxSortOrderByWatchlistId(@Param("watchlistId") UUID watchlistId);
}
