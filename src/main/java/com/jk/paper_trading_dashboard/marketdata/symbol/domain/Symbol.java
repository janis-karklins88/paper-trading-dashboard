package com.jk.paper_trading_dashboard.marketdata.symbol.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.jk.paper_trading_dashboard.marketdata.domain.Symbols;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "symbols")
public class Symbol {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String symbol;

  @Column(nullable = false)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AssetType assetType;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private boolean tradable;

  private String exchange;

  private Instant createdAt;

  private Instant updatedAt;

  protected Symbol() {

  }

  public Symbol(String symbol, String displayName, AssetType assetType, String exchange, boolean active,
      boolean tradable) {
    this.symbol = Symbols.normalize(symbol);
    this.displayName = requireDisplayName(displayName, this.symbol);
    this.assetType = Objects.requireNonNull(assetType, "assetType is required");
    this.exchange = normalizeNullable(exchange);
    this.active = active;
    this.tradable = tradable;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public void updateFrom(Symbol source) {
    Objects.requireNonNull(source, "source is required");

    this.displayName = source.displayName;
    this.assetType = source.assetType;
    this.exchange = source.exchange;
    this.active = source.active;
    this.tradable = source.tradable;
    this.updatedAt = Instant.now();
  }

  private static String requireDisplayName(String displayName, String fallbackSymbol) {
    String normalizedDisplayName = normalizeNullable(displayName);
    return normalizedDisplayName == null ? fallbackSymbol : normalizedDisplayName;
  }

  private static String normalizeNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return value.trim();
  }

  @PrePersist
  private void prePersist() {
    Instant now = Instant.now();

    if (createdAt == null) {
      createdAt = now;
    }

    if (updatedAt == null) {
      updatedAt = createdAt;
    }
  }

  @PreUpdate
  private void preUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = Symbols.normalize(symbol);
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = requireDisplayName(displayName, this.symbol);
  }

  public AssetType getAssetType() {
    return assetType;
  }

  public void setAssetType(AssetType assetType) {
    this.assetType = assetType;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isTradable() {
    return tradable;
  }

  public void setTradable(boolean tradable) {
    this.tradable = tradable;
  }

  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = normalizeNullable(exchange);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
