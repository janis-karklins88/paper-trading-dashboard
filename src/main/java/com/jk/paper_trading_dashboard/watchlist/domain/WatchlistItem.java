package com.jk.paper_trading_dashboard.watchlist.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.jk.paper_trading_dashboard.marketdata.domain.Symbols;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "watchlist_items")
public class WatchlistItem {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "watchlist_id", nullable = false)
  private Watchlist watchlist;

  @Column(nullable = false)
  private String symbol;

  @Column(nullable = false)
  private int sortOrder;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  protected WatchlistItem() {

  }

  public WatchlistItem(Watchlist watchlist, String symbol, int sortOrder) {
    this.watchlist = Objects.requireNonNull(watchlist, "watchlist is required");
    this.symbol = Symbols.normalize(symbol);
    this.sortOrder = sortOrder;
    this.createdAt = Instant.now();
  }

  @PrePersist
  private void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Watchlist getWatchlist() {
    return watchlist;
  }

  public void setWatchlist(Watchlist watchlist) {
    this.watchlist = watchlist;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = Symbols.normalize(symbol);
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
