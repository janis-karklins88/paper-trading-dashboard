package com.jk.paper_trading_dashboard.watchlist.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.jk.paper_trading_dashboard.user.domain.User;

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
@Table(name = "watchlists")
public class Watchlist {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  protected Watchlist() {

  }

  public Watchlist(User user, String name) {
    this.user = Objects.requireNonNull(user, "user is required");
    this.name = requireName(name);
    this.createdAt = Instant.now();
  }

  private static String requireName(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }

    return value.trim();
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

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = requireName(name);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
