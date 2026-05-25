package com.jk.paper_trading_dashboard.position.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.jk.paper_trading_dashboard.marketdata.domain.Symbols;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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

  @Column(precision = 19, scale = 8)
  private BigDecimal takeProfitPrice;

  @Column(precision = 19, scale = 8)
  private BigDecimal stopLossPrice;

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

  @Version
  @Column(nullable = false)
  private Long version;

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
    this(
        tradingAccountId,
        symbol,
        side,
        quantity,
        avgEntryPrice,
        currentPrice,
        marginUsed,
        leverage,
        null,
        null);
  }

  public Position(
      UUID tradingAccountId,
      String symbol,
      PositionSide side,
      BigDecimal quantity,
      BigDecimal avgEntryPrice,
      BigDecimal currentPrice,
      BigDecimal marginUsed,
      BigDecimal leverage,
      BigDecimal takeProfitPrice,
      BigDecimal stopLossPrice) {
    requirePositive(quantity, "quantity");
    requirePositive(avgEntryPrice, "avgEntryPrice");
    requirePositive(currentPrice, "currentPrice");
    requirePositive(marginUsed, "marginUsed");
    requirePositive(leverage, "leverage");
    requirePositiveIfPresent(takeProfitPrice, "takeProfitPrice");
    requirePositiveIfPresent(stopLossPrice, "stopLossPrice");

    this.id = UUID.randomUUID();
    this.tradingAccountId = Objects.requireNonNull(tradingAccountId, "tradingAccountId is required");
    this.symbol = Symbols.normalize(symbol);
    this.side = Objects.requireNonNull(side, "side is required");
    validateExitPrices(this.side, avgEntryPrice, takeProfitPrice, stopLossPrice);
    this.quantity = quantity;
    this.avgEntryPrice = avgEntryPrice;
    this.currentPrice = currentPrice;
    this.marginUsed = marginUsed;
    this.leverage = leverage;
    this.takeProfitPrice = takeProfitPrice;
    this.stopLossPrice = stopLossPrice;
    this.unrealizedPnl = calculateUnrealizedPnl();
    this.realizedPnl = BigDecimal.ZERO;
    this.status = PositionStatus.OPEN;
    this.openedAt = Instant.now();
    this.updatedAt = this.openedAt;
    this.version = 0L;
  }

  public BigDecimal close() {
    if (this.status != PositionStatus.OPEN) {
      throw new IllegalStateException("Only open positions can be closed");
    }

    BigDecimal closingPnl = calculateUnrealizedPnl();
    this.realizedPnl = this.realizedPnl.add(closingPnl);
    this.unrealizedPnl = BigDecimal.ZERO;
    this.status = PositionStatus.CLOSED;
    this.closedAt = Instant.now();
    this.updatedAt = this.closedAt;
    return closingPnl;
  }

  public BigDecimal calculateUnrealizedPnl(BigDecimal currentPrice) {
    requirePositive(currentPrice, "currentPrice");

    return switch (side) {
      case LONG -> currentPrice.subtract(avgEntryPrice).multiply(quantity);
      case SHORT -> avgEntryPrice.subtract(currentPrice).multiply(quantity);
    };
  }

  public boolean shouldTakeProfit(BigDecimal marketPrice) {
    if (takeProfitPrice == null) {
      return false;
    }

    requirePositive(marketPrice, "marketPrice");

    return switch (side) {
      case LONG -> marketPrice.compareTo(takeProfitPrice) >= 0;
      case SHORT -> marketPrice.compareTo(takeProfitPrice) <= 0;
    };
  }

  public boolean shouldStopLoss(BigDecimal marketPrice) {
    if (stopLossPrice == null) {
      return false;
    }

    requirePositive(marketPrice, "marketPrice");

    return switch (side) {
      case LONG -> marketPrice.compareTo(stopLossPrice) <= 0;
      case SHORT -> marketPrice.compareTo(stopLossPrice) >= 0;
    };
  }

  public void updateExitPrices(BigDecimal takeProfitPrice, BigDecimal stopLossPrice) {
    if (this.status != PositionStatus.OPEN) {
      throw new IllegalStateException("Only open positions can update exit prices");
    }

    requirePositiveIfPresent(takeProfitPrice, "takeProfitPrice");
    requirePositiveIfPresent(stopLossPrice, "stopLossPrice");
    validateExitPrices(this.side, this.avgEntryPrice, takeProfitPrice, stopLossPrice);

    this.takeProfitPrice = takeProfitPrice;
    this.stopLossPrice = stopLossPrice;
    this.updatedAt = Instant.now();
  }

  private BigDecimal calculateUnrealizedPnl() {
    return calculateUnrealizedPnl(currentPrice);
  }

  private static void requirePositive(BigDecimal value, String fieldName) {
    if (value == null || value.signum() <= 0) {
      throw new IllegalArgumentException(fieldName + " must be greater than zero");
    }
  }

  private static void requirePositiveIfPresent(BigDecimal value, String fieldName) {
    if (value != null && value.signum() <= 0) {
      throw new IllegalArgumentException(fieldName + " must be greater than zero");
    }
  }

  private static void validateExitPrices(
      PositionSide side,
      BigDecimal entryPrice,
      BigDecimal takeProfitPrice,
      BigDecimal stopLossPrice) {
    if (side == PositionSide.LONG) {
      if (takeProfitPrice != null && takeProfitPrice.compareTo(entryPrice) <= 0) {
        throw new IllegalArgumentException("takeProfitPrice must be greater than entry price for long positions");
      }

      if (stopLossPrice != null && stopLossPrice.compareTo(entryPrice) >= 0) {
        throw new IllegalArgumentException("stopLossPrice must be less than entry price for long positions");
      }

      return;
    }

    if (takeProfitPrice != null && takeProfitPrice.compareTo(entryPrice) >= 0) {
      throw new IllegalArgumentException("takeProfitPrice must be less than entry price for short positions");
    }

    if (stopLossPrice != null && stopLossPrice.compareTo(entryPrice) <= 0) {
      throw new IllegalArgumentException("stopLossPrice must be greater than entry price for short positions");
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

  public BigDecimal getTakeProfitPrice() {
    return takeProfitPrice;
  }

  public void setTakeProfitPrice(BigDecimal takeProfitPrice) {
    this.takeProfitPrice = takeProfitPrice;
  }

  public BigDecimal getStopLossPrice() {
    return stopLossPrice;
  }

  public void setStopLossPrice(BigDecimal stopLossPrice) {
    this.stopLossPrice = stopLossPrice;
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

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
