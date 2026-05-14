package com.jk.paper_trading_dashboard.order.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.domain.OrderType;

public interface OrderRepository extends JpaRepository<Order, UUID> {

  List<Order> findByTradingAccountIdOrderByCreatedAtDesc(UUID tradingAccountId);

  Optional<Order> findByIdAndTradingAccountId(UUID id, UUID tradingAccountId);

  boolean existsByTradingAccountIdAndStatusIn(UUID tradingAccountId, Collection<OrderStatus> statuses);

  List<Order> findByStatusAndType(OrderStatus status, OrderType type);
}
