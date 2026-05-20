package com.jk.paper_trading_dashboard.watchlist.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.jk.paper_trading_dashboard.watchlist.domain.Watchlist;

public record WatchlistDetailResponse(
    UUID id,
    UUID userId,
    String name,
    boolean defaultWatchlist,
    Instant createdAt,
    List<WatchlistItemResponse> items) {

  public static WatchlistDetailResponse from(Watchlist watchlist, List<WatchlistItemResponse> items) {
    return new WatchlistDetailResponse(
        watchlist.getId(),
        watchlist.getUser().getId(),
        watchlist.getName(),
        watchlist.isDefaultWatchlist(),
        watchlist.getCreatedAt(),
        items);
  }
}
