package com.jk.paper_trading_dashboard.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jk.paper_trading_dashboard.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {

  List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

  Optional<Order> findByIdAndUserId(UUID id, UUID userId);
}
