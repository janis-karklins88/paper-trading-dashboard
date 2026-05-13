package com.jk.paper_trading_dashboard.account.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.jk.paper_trading_dashboard.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "trading_accounts")
public class TradingAccount {

  private static final BigDecimal DEFAULT_STARTING_CASH = new BigDecimal("100000");
  private static final BigDecimal DEFAULT_MAX_LEVERAGE = new BigDecimal("5");

  @Id
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal startingCash;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal cashBalance;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal reservedMargin;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal realizedPnl;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal unrealizedPnl;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal maxLeverage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TradingAccountStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected TradingAccount() {

  }

  public TradingAccount(User user) {
    this.id = UUID.randomUUID();
    this.user = Objects.requireNonNull(user, "user is required");
    this.startingCash = DEFAULT_STARTING_CASH;
    this.cashBalance = DEFAULT_STARTING_CASH;
    this.reservedMargin = BigDecimal.ZERO;
    this.realizedPnl = BigDecimal.ZERO;
    this.unrealizedPnl = BigDecimal.ZERO;
    this.maxLeverage = DEFAULT_MAX_LEVERAGE;
    this.status = TradingAccountStatus.ACTIVE;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public void reset() {

    this.cashBalance = this.startingCash;
    this.reservedMargin = BigDecimal.ZERO;
    this.realizedPnl = BigDecimal.ZERO;
    this.unrealizedPnl = BigDecimal.ZERO;
    this.updatedAt = Instant.now();
  }

  public void reserveMargin(BigDecimal marginAmount) {
    if (marginAmount == null || marginAmount.signum() <= 0) {
      throw new IllegalArgumentException("marginAmount must be greater than zero");
    }

    if (cashBalance.compareTo(marginAmount) < 0) {
      throw new IllegalArgumentException("Insufficient cash balance");
    }

    this.cashBalance = this.cashBalance.subtract(marginAmount);
    this.reservedMargin = this.reservedMargin.add(marginAmount);
    this.updatedAt = Instant.now();
  }

  public void deductFee(BigDecimal feeAmount) {
    if (feeAmount == null || feeAmount.signum() < 0) {
      throw new IllegalArgumentException("feeAmount must be zero or greater");
    }

    if (cashBalance.compareTo(feeAmount) < 0) {
      throw new IllegalArgumentException("Insufficient cash balance for fee");
    }

    this.cashBalance = this.cashBalance.subtract(feeAmount);
    this.updatedAt = Instant.now();
  }

  public void releaseMargin(BigDecimal marginAmount) {
    if (marginAmount == null || marginAmount.signum() <= 0) {
      throw new IllegalArgumentException("marginAmount must be greater than zero");
    }

    BigDecimal releasedMargin = marginAmount.min(this.reservedMargin);
    this.reservedMargin = this.reservedMargin.subtract(releasedMargin);
    this.cashBalance = this.cashBalance.add(releasedMargin);
    this.updatedAt = Instant.now();
  }

  public void applyPositionOpen(BigDecimal unrealizedPnl) {
    if (unrealizedPnl == null) {
      throw new IllegalArgumentException("unrealizedPnl is required");
    }

    this.unrealizedPnl = this.unrealizedPnl.add(unrealizedPnl);
    this.updatedAt = Instant.now();
  }

  public void applyPositionClose(BigDecimal realizedPnl, BigDecimal releasedMargin, BigDecimal closedUnrealizedPnl) {
    if (realizedPnl == null) {
      throw new IllegalArgumentException("realizedPnl is required");
    }

    if (releasedMargin == null || releasedMargin.signum() < 0) {
      throw new IllegalArgumentException("releasedMargin must be zero or greater");
    }

    if (closedUnrealizedPnl == null) {
      throw new IllegalArgumentException("closedUnrealizedPnl is required");
    }

    this.cashBalance = this.cashBalance.add(releasedMargin).add(realizedPnl);
    this.reservedMargin = this.reservedMargin.subtract(releasedMargin);
    if (this.reservedMargin.signum() < 0) {
      this.reservedMargin = BigDecimal.ZERO;
    }
    this.realizedPnl = this.realizedPnl.add(realizedPnl);
    this.unrealizedPnl = this.unrealizedPnl.subtract(closedUnrealizedPnl);
    this.updatedAt = Instant.now();
  }

  public BigDecimal getEquity() {
    return cashBalance
        .add(unrealizedPnl);
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public BigDecimal getStartingCash() {
    return startingCash;
  }

  public void setStartingCash(BigDecimal startingCash) {
    this.startingCash = startingCash;
  }

  public BigDecimal getCashBalance() {
    return cashBalance;
  }

  public void setCashBalance(BigDecimal cashBalance) {
    this.cashBalance = cashBalance;
  }

  public BigDecimal getReservedMargin() {
    return reservedMargin;
  }

  public void setReservedMargin(BigDecimal reservedMargin) {
    this.reservedMargin = reservedMargin;
  }

  public BigDecimal getRealizedPnl() {
    return realizedPnl;
  }

  public void setRealizedPnl(BigDecimal realizedPnl) {
    this.realizedPnl = realizedPnl;
  }

  public BigDecimal getUnrealizedPnl() {
    return unrealizedPnl;
  }

  public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
    this.unrealizedPnl = unrealizedPnl;
  }

  public BigDecimal getMaxLeverage() {
    return maxLeverage;
  }

  public void setMaxLeverage(BigDecimal maxLeverage) {
    this.maxLeverage = maxLeverage;
  }

  public TradingAccountStatus getStatus() {
    return status;
  }

  public void setStatus(TradingAccountStatus status) {
    this.status = status;
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
