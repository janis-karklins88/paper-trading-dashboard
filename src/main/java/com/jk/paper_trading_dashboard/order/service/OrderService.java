package com.jk.paper_trading_dashboard.order.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.service.TradingAccountService;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.domain.OrderType;
import com.jk.paper_trading_dashboard.order.dto.OrderResponse;
import com.jk.paper_trading_dashboard.order.dto.PlaceOrderRequest;
import com.jk.paper_trading_dashboard.order.exception.InvalidOrderException;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final TradingAccountService tradingAccountService;

  public OrderService(
      OrderRepository orderRepository,
      TradingAccountService tradingAccountService) {
    this.orderRepository = orderRepository;
    this.tradingAccountService = tradingAccountService;
  }

  @Transactional
  public OrderResponse placeOrder(UUID userId, PlaceOrderRequest request) {
    Order order = createPendingOrder(userId, request);
    orderRepository.save(order);

    return OrderResponse.from(order);
  }

  public List<OrderResponse> getOrders(UUID userId) {
    UUID tradingAccountId = getTradingAccountId(userId);

    return orderRepository.findByTradingAccountIdOrderByCreatedAtDesc(tradingAccountId)
        .stream()
        .map(OrderResponse::from)
        .toList();
  }

  public OrderResponse getOrder(UUID userId, UUID orderId) {
    Order order = getUserOrder(userId, orderId);
    return OrderResponse.from(order);
  }

  @Transactional
  public OrderResponse cancelOrder(UUID userId, UUID orderId) {
    Order order = getUserOrder(userId, orderId);

    if (!isCancelable(order.getStatus())) {
      throw new InvalidOrderException("Order cannot be canceled from status " + order.getStatus());
    }

    order.markCanceled();
    return OrderResponse.from(order);
  }

  private Order createPendingOrder(UUID userId, PlaceOrderRequest request) {
    UUID tradingAccountId = getTradingAccountId(userId);

    if (request.type() == OrderType.MARKET) {
      if (request.limitPrice() != null) {
        throw new InvalidOrderException("Market order cannot have limit price");
      }

      return Order.pendingMarketOrder(
          tradingAccountId,
          request.symbol(),
          request.side(),
          request.marginAmount(),
          request.leverage(),
          request.takeProfitPrice(),
          request.stopLossPrice());
    }

    if (request.type() == OrderType.LIMIT) {
      if (request.limitPrice() == null) {
        throw new InvalidOrderException("Limit order requires limit price");
      }

      return Order.pendingLimitOrder(
          tradingAccountId,
          request.symbol(),
          request.side(),
          request.marginAmount(),
          request.leverage(),
          request.limitPrice(),
          request.takeProfitPrice(),
          request.stopLossPrice());
    }

    throw new InvalidOrderException("Unsupported order type");
  }

  private UUID getTradingAccountId(UUID userId) {
    return tradingAccountService.getActiveAccount(userId).getId();
  }

  private Order getUserOrder(UUID userId, UUID orderId) {
    UUID tradingAccountId = getTradingAccountId(userId);

    return orderRepository.findByIdAndTradingAccountId(orderId, tradingAccountId)
        .orElseThrow(() -> new NotFoundException("Order not found"));
  }

  private boolean isCancelable(OrderStatus status) {
    return switch (status) {
      case PENDING, OPEN -> true;
      case FILLED, REJECTED, CANCELED -> false;
    };
  }

}
