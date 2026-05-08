package com.jk.paper_trading_dashboard.order.domain;

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
@Table(name = "orders")
public class Order {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, updatable = false)
  private UUID userId;

  @Column(nullable = false)
  private String symbol;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderSide side;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderType type;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal quantity;

  @Column(precision = 19, scale = 8)
  private BigDecimal limitPrice;

  @Column(precision = 19, scale = 8)
  private BigDecimal stopPrice;

  @Column(precision = 19, scale = 8)
  private BigDecimal takeProfitPrice;

  @Column(precision = 19, scale = 8)
  private BigDecimal stopLossPrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  private String brokerOrderId;

  @Column(nullable = false)
  private UUID brokerAccountId;

  private String brokerOrderStatus;

  @Column(precision = 19, scale = 8)
  private BigDecimal filledQuantity;

  private String rejectReason;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant submittedAt;

  private Instant filledAt;

  private Instant updatedAt;

  protected Order() {

  }

  public static Order pendingMarketOrder(
      UUID userId,
      UUID brokerAccountId,
      String symbol,
      OrderSide side,
      BigDecimal quantity) {
    Order order = pendingOrder(userId, brokerAccountId, symbol, side, OrderType.MARKET, quantity);
    return order;
  }

  public static Order pendingLimitOrder(
      UUID userId,
      UUID brokerAccountId,
      String symbol,
      OrderSide side,
      BigDecimal quantity,
      BigDecimal limitPrice) {
    requirePositive(limitPrice, "limitPrice");

    Order order = pendingOrder(userId, brokerAccountId, symbol, side, OrderType.LIMIT, quantity);
    order.limitPrice = limitPrice;
    return order;
  }

  private static Order pendingOrder(
      UUID userId,
      UUID brokerAccountId,
      String symbol,
      OrderSide side,
      OrderType type,
      BigDecimal quantity) {
    requirePositive(quantity, "quantity");

    Order order = new Order();
    order.id = UUID.randomUUID();
    order.userId = Objects.requireNonNull(userId, "userId is required");
    order.symbol = requireText(symbol, "symbol").toUpperCase();
    order.side = Objects.requireNonNull(side, "side is required");
    order.type = Objects.requireNonNull(type, "type is required");
    order.quantity = quantity;
    order.status = OrderStatus.PENDING;
    order.brokerAccountId = Objects.requireNonNull(brokerAccountId, "brokerAccountId is required");
    order.filledQuantity = BigDecimal.ZERO;
    order.createdAt = Instant.now();
    order.updatedAt = order.createdAt;
    return order;
  }

  public void markSubmitted(String brokerOrderId, String brokerOrderStatus) {

    if (this.status != OrderStatus.PENDING) {
      throw new IllegalStateException(
          "Only pending orders can be submitted");
    }

    this.status = OrderStatus.SUBMITTED;
    this.brokerOrderId = requireText(brokerOrderId, "brokerOrderId");
    this.brokerOrderStatus = requireText(brokerOrderStatus, "brokerOrderStatus");
    this.submittedAt = Instant.now();
    touch();
  }

  public void markAccepted(String brokerOrderStatus) {
    this.status = OrderStatus.ACCEPTED;
    this.brokerOrderStatus = brokerOrderStatus;
    touch();
  }

  public void markPartiallyFilled(BigDecimal filledQuantity, String brokerOrderStatus) {
    requirePositive(filledQuantity, "filledQuantity");

    this.status = OrderStatus.PARTIALLY_FILLED;
    this.filledQuantity = filledQuantity;
    this.brokerOrderStatus = brokerOrderStatus;
    touch();
  }

  public void markFilled(BigDecimal filledQuantity, String brokerOrderStatus) {
    requirePositive(filledQuantity, "filledQuantity");

    this.status = OrderStatus.FILLED;
    this.filledQuantity = filledQuantity;
    this.brokerOrderStatus = brokerOrderStatus;
    this.filledAt = Instant.now();
    touch();
  }

  public void markRejected(String rejectReason, String brokerOrderStatus) {
    this.status = OrderStatus.REJECTED;
    this.rejectReason = rejectReason;
    this.brokerOrderStatus = brokerOrderStatus;
    touch();
  }

  public void markCanceled(String brokerOrderStatus) {
    this.status = OrderStatus.CANCELED;
    this.brokerOrderStatus = brokerOrderStatus;
    touch();
  }

  public void markFailed(String rejectReason) {
    this.status = OrderStatus.FAILED;
    this.rejectReason = rejectReason;
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

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
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

  public BigDecimal getLimitPrice() {
    return limitPrice;
  }

  public void setLimitPrice(BigDecimal limitPrice) {
    this.limitPrice = limitPrice;
  }

  public BigDecimal getStopPrice() {
    return stopPrice;
  }

  public void setStopPrice(BigDecimal stopPrice) {
    this.stopPrice = stopPrice;
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

  public String getBrokerOrderId() {
    return brokerOrderId;
  }

  public void setBrokerOrderId(String brokerOrderId) {
    this.brokerOrderId = brokerOrderId;
  }

  public UUID getBrokerAccountId() {
    return brokerAccountId;
  }

  public void setBrokerAccountId(UUID brokerAccountId) {
    this.brokerAccountId = brokerAccountId;
  }

  public String getBrokerOrderStatus() {
    return brokerOrderStatus;
  }

  public void setBrokerOrderStatus(String brokerOrderStatus) {
    this.brokerOrderStatus = brokerOrderStatus;
  }

  public BigDecimal getFilledQuantity() {
    return filledQuantity;
  }

  public void setFilledQuantity(BigDecimal filledQuantity) {
    this.filledQuantity = filledQuantity;
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

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(Instant submittedAt) {
    this.submittedAt = submittedAt;
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
