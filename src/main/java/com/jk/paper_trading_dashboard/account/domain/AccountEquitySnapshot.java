package com.jk.paper_trading_dashboard.account.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.jk.paper_trading_dashboard.account.dto.TradingAccountResponse;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_equity_snapshots")
public class AccountEquitySnapshot {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID accountId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal cashBalance;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal reservedMargin;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal realizedPnl;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal unrealizedPnl;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal totalEquity;

  @Column(nullable = false)
  private Instant createdAt;

  protected AccountEquitySnapshot() {

  }

  public AccountEquitySnapshot(TradingAccount account, TradingAccountResponse accountSummary) {
    Objects.requireNonNull(account, "account is required");
    Objects.requireNonNull(accountSummary, "accountSummary is required");

    this.id = UUID.randomUUID();
    this.accountId = account.getId();
    this.cashBalance = accountSummary.cashBalance();
    this.reservedMargin = accountSummary.reservedMargin();
    this.realizedPnl = accountSummary.realizedPnl();
    this.unrealizedPnl = accountSummary.unrealizedPnl();
    this.totalEquity = accountSummary.equity();
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
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

  public BigDecimal getTotalEquity() {
    return totalEquity;
  }

  public void setTotalEquity(BigDecimal totalEquity) {
    this.totalEquity = totalEquity;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
