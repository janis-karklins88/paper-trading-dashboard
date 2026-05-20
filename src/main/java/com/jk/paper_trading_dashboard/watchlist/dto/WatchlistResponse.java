package com.jk.paper_trading_dashboard.watchlist.dto;

import java.time.Instant;
import java.util.UUID;

import com.jk.paper_trading_dashboard.watchlist.domain.Watchlist;

public record WatchlistResponse(
    UUID id,
    UUID userId,
    String name,
    Instant createdAt) {

  public static WatchlistResponse from(Watchlist watchlist) {
    return new WatchlistResponse(
        watchlist.getId(),
        watchlist.getUser().getId(),
        watchlist.getName(),
        watchlist.getCreatedAt());
  }
}
