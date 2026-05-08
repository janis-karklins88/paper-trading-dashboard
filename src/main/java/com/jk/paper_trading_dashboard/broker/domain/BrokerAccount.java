package com.jk.paper_trading_dashboard.broker.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "broker_accounts")
public class BrokerAccount {

  @Id
  private UUID id;

  @Column(nullable = false, unique = true)
  private UUID userId;

  @Column(nullable = false, unique = true)
  private String alpacaAccountId;

  @Column(nullable = false)
  private boolean active;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected BrokerAccount() {
  }

  public BrokerAccount(UUID userId, String alpacaAccountId) {
    this.id = UUID.randomUUID();
    this.userId = userId;
    this.alpacaAccountId = alpacaAccountId;
    this.active = true;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public void deactivate() {
    this.active = false;
    this.updatedAt = Instant.now();
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

  public String getAlpacaAccountId() {
    return alpacaAccountId;
  }

  public void setAlpacaAccountId(String alpacaAccountId) {
    this.alpacaAccountId = alpacaAccountId;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
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
