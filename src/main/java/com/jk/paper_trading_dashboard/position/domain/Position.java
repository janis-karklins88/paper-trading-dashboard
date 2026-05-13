package com.jk.paper_trading_dashboard.position.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "positions")
public class Position {
  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false)
  private UUID tradingAccountId;

  @Column(nullable = false)
  private String symbol;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PositionSide side;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal quantity;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal avgEntryPrice;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal currentPrice;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal marginUsed;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal leverage;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal unrealizedPnl;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal realizedPnl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PositionStatus status;

  @Column(nullable = false)
  private Instant openedAt;

  private Instant closedAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected Position() {

  }

  public Position(
      UUID tradingAccountId,
      String symbol,
      PositionSide side,
      BigDecimal quantity,
      BigDecimal avgEntryPrice,
      BigDecimal currentPrice,
      BigDecimal marginUsed,
      BigDecimal leverage) {
    requirePositive(quantity, "quantity");
    requirePositive(avgEntryPrice, "avgEntryPrice");
    requirePositive(currentPrice, "currentPrice");
    requirePositive(marginUsed, "marginUsed");
    requirePositive(leverage, "leverage");

    this.id = UUID.randomUUID();
    this.tradingAccountId = Objects.requireNonNull(tradingAccountId, "tradingAccountId is required");
    this.symbol = requireText(symbol, "symbol").toUpperCase();
    this.side = Objects.requireNonNull(side, "side is required");
    this.quantity = quantity;
    this.avgEntryPrice = avgEntryPrice;
    this.currentPrice = currentPrice;
    this.marginUsed = marginUsed;
    this.leverage = leverage;
    this.unrealizedPnl = calculateUnrealizedPnl();
    this.realizedPnl = BigDecimal.ZERO;
    this.status = PositionStatus.OPEN;
    this.openedAt = Instant.now();
    this.updatedAt = this.openedAt;
  }

  private BigDecimal calculateUnrealizedPnl() {
    return switch (side) {
      case LONG -> currentPrice.subtract(avgEntryPrice).multiply(quantity);
      case SHORT -> avgEntryPrice.subtract(currentPrice).multiply(quantity);
    };
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }

    return value.trim();
  }

  private static void requirePositive(BigDecimal value, String fieldName) {
    if (value == null || value.signum() <= 0) {
      throw new IllegalArgumentException(fieldName + " must be greater than zero");
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getTradingAccountId() {
    return tradingAccountId;
  }

  public void setTradingAccountId(UUID tradingAccountId) {
    this.tradingAccountId = tradingAccountId;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public PositionSide getSide() {
    return side;
  }

  public void setSide(PositionSide side) {
    this.side = side;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getAvgEntryPrice() {
    return avgEntryPrice;
  }

  public void setAvgEntryPrice(BigDecimal avgEntryPrice) {
    this.avgEntryPrice = avgEntryPrice;
  }

  public BigDecimal getCurrentPrice() {
    return currentPrice;
  }

  public void setCurrentPrice(BigDecimal currentPrice) {
    this.currentPrice = currentPrice;
  }

  public BigDecimal getMarginUsed() {
    return marginUsed;
  }

  public void setMarginUsed(BigDecimal marginUsed) {
    this.marginUsed = marginUsed;
  }

  public BigDecimal getLeverage() {
    return leverage;
  }

  public void setLeverage(BigDecimal leverage) {
    this.leverage = leverage;
  }

  public BigDecimal getUnrealizedPnl() {
    return unrealizedPnl;
  }

  public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
    this.unrealizedPnl = unrealizedPnl;
  }

  public BigDecimal getRealizedPnl() {
    return realizedPnl;
  }

  public void setRealizedPnl(BigDecimal realizedPnl) {
    this.realizedPnl = realizedPnl;
  }

  public PositionStatus getStatus() {
    return status;
  }

  public void setStatus(PositionStatus status) {
    this.status = status;
  }

  public Instant getOpenedAt() {
    return openedAt;
  }

  public void setOpenedAt(Instant openedAt) {
    this.openedAt = openedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(Instant closedAt) {
    this.closedAt = closedAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
