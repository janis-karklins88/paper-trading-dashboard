package com.jk.paper_trading_dashboard.watchlist.dto;

import java.time.Instant;
import java.util.UUID;

import com.jk.paper_trading_dashboard.watchlist.domain.WatchlistItem;

public record WatchlistItemResponse(
    UUID id,
    UUID watchlistId,
    String symbol,
    int sortOrder,
    Instant createdAt) {

  public static WatchlistItemResponse from(WatchlistItem item) {
    return new WatchlistItemResponse(
        item.getId(),
        item.getWatchlist().getId(),
        item.getSymbol(),
        item.getSortOrder(),
        item.getCreatedAt());
  }
}
