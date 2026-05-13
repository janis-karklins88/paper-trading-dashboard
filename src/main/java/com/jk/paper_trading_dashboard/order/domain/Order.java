package com.jk.paper_trading_dashboard.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
@Table(name = "orders")
public class Order {

  private static final int QUANTITY_SCALE = 8;

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, updatable = false)
  private UUID tradingAccountId;

  @Column(nullable = false)
  private String symbol;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderSide side;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderType type;

  @Column(precision = 19, scale = 8)
  private BigDecimal quantity;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal marginAmount;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal leverage;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal notionalValue;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal feeAmount;

  @Column(precision = 19, scale = 8)
  private BigDecimal filledPrice;

  @Column(precision = 19, scale = 8)
  private BigDecimal limitPrice;

  @Column(precision = 19, scale = 8)
  private BigDecimal takeProfitPrice;

  @Column(precision = 19, scale = 8)
  private BigDecimal stopLossPrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  private String rejectReason;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant openedAt;

  private Instant filledAt;

  private Instant updatedAt;

  protected Order() {

  }

  public static Order pendingMarketOrder(
      UUID tradingAccountId,
      String symbol,
      OrderSide side,
      BigDecimal marginAmount,
      BigDecimal leverage,
      BigDecimal takeProfitPrice,
      BigDecimal stopLossPrice) {
    return pendingOrder(
        tradingAccountId,
        symbol,
        side,
        OrderType.MARKET,
        marginAmount,
        leverage,
        null,
        takeProfitPrice,
        stopLossPrice);
  }

  public static Order pendingLimitOrder(
      UUID tradingAccountId,
      String symbol,
      OrderSide side,
      BigDecimal marginAmount,
      BigDecimal leverage,
      BigDecimal limitPrice,
      BigDecimal takeProfitPrice,
      BigDecimal stopLossPrice) {
    requirePositive(limitPrice, "limitPrice");

    return pendingOrder(
        tradingAccountId,
        symbol,
        side,
        OrderType.LIMIT,
        marginAmount,
        leverage,
        limitPrice,
        takeProfitPrice,
        stopLossPrice);
  }

  private static Order pendingOrder(
      UUID tradingAccountId,
      String symbol,
      OrderSide side,
      OrderType type,
      BigDecimal marginAmount,
      BigDecimal leverage,
      BigDecimal limitPrice,
      BigDecimal takeProfitPrice,
      BigDecimal stopLossPrice) {
    requirePositive(marginAmount, "marginAmount");
    requirePositive(leverage, "leverage");

    Order order = new Order();
    order.id = UUID.randomUUID();
    order.tradingAccountId = Objects.requireNonNull(tradingAccountId, "tradingAccountId is required");
    order.symbol = requireText(symbol, "symbol").toUpperCase();
    order.side = Objects.requireNonNull(side, "side is required");
    order.type = Objects.requireNonNull(type, "type is required");
    order.marginAmount = marginAmount;
    order.leverage = leverage;
    order.notionalValue = marginAmount.multiply(leverage);
    order.feeAmount = BigDecimal.ZERO;
    order.limitPrice = limitPrice;
    order.takeProfitPrice = takeProfitPrice;
    order.stopLossPrice = stopLossPrice;
    order.status = OrderStatus.PENDING;
    order.createdAt = Instant.now();
    order.updatedAt = order.createdAt;
    return order;
  }

  public void markOpen() {
    if (this.status != OrderStatus.PENDING) {
      throw new IllegalStateException("Only pending orders can be opened");
    }

    this.status = OrderStatus.OPEN;
    this.openedAt = Instant.now();
    touch();
  }

  public void markFilled(BigDecimal filledPrice) {
    if (this.status != OrderStatus.PENDING && this.status != OrderStatus.OPEN) {
      throw new IllegalStateException("Only pending or open orders can be filled");
    }

    requirePositive(filledPrice, "filledPrice");

    this.status = OrderStatus.FILLED;
    this.filledPrice = filledPrice;
    this.quantity = this.notionalValue.divide(filledPrice, QUANTITY_SCALE, RoundingMode.HALF_UP);
    this.filledAt = Instant.now();
    touch();
  }

  public void applyFee(BigDecimal feeAmount) {
    if (feeAmount == null || feeAmount.signum() < 0) {
      throw new IllegalArgumentException("feeAmount must be zero or greater");
    }

    this.feeAmount = feeAmount;
    touch();
  }

  public void markCanceled() {
    if (this.status == OrderStatus.FILLED || this.status == OrderStatus.REJECTED) {
      throw new IllegalStateException("Filled or rejected orders cannot be canceled");
    }

    this.status = OrderStatus.CANCELED;
    touch();
  }

  public void markRejected(String rejectReason) {
    if (this.status == OrderStatus.FILLED || this.status == OrderStatus.CANCELED) {
      throw new IllegalStateException("Filled or canceled orders cannot be rejected");
    }

    this.status = OrderStatus.REJECTED;
    this.rejectReason = requireText(rejectReason, "rejectReason");
    touch();
  }

  private void touch() {
    this.updatedAt = Instant.now();
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

  public OrderSide getSide() {
    return side;
  }

  public void setSide(OrderSide side) {
    this.side = side;
  }

  public OrderType getType() {
    return type;
  }

  public void setType(OrderType type) {
    this.type = type;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getMarginAmount() {
    return marginAmount;
  }

  public void setMarginAmount(BigDecimal marginAmount) {
    this.marginAmount = marginAmount;
  }

  public BigDecimal getLeverage() {
    return leverage;
  }

  public void setLeverage(BigDecimal leverage) {
    this.leverage = leverage;
  }

  public BigDecimal getNotionalValue() {
    return notionalValue;
  }

  public void setNotionalValue(BigDecimal notionalValue) {
    this.notionalValue = notionalValue;
  }

  public BigDecimal getFeeAmount() {
    return feeAmount;
  }

  public void setFeeAmount(BigDecimal feeAmount) {
    this.feeAmount = feeAmount;
  }

  public BigDecimal getFilledPrice() {
    return filledPrice;
  }

  public void setFilledPrice(BigDecimal filledPrice) {
    this.filledPrice = filledPrice;
  }

  public BigDecimal getLimitPrice() {
    return limitPrice;
  }

  public void setLimitPrice(BigDecimal limitPrice) {
    this.limitPrice = limitPrice;
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

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  public void setRejectReason(String rejectReason) {
    this.rejectReason = rejectReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getOpenedAt() {
    return openedAt;
  }

  public void setOpenedAt(Instant openedAt) {
    this.openedAt = openedAt;
  }

  public Instant getFilledAt() {
    return filledAt;
  }

  public void setFilledAt(Instant filledAt) {
    this.filledAt = filledAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
