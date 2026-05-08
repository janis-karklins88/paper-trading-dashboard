package com.jk.paper_trading_dashboard.order.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.alpaca.BrokerClient;
import com.jk.paper_trading_dashboard.alpaca.dto.BrokerOrderRequest;
import com.jk.paper_trading_dashboard.alpaca.dto.BrokerOrderResponse;
import com.jk.paper_trading_dashboard.broker.repository.BrokerAccountRepository;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.domain.OrderType;
import com.jk.paper_trading_dashboard.order.dto.OrderResponse;
import com.jk.paper_trading_dashboard.order.dto.PlaceOrderRequest;
import com.jk.paper_trading_dashboard.order.exception.BrokerOrderRejectedException;
import com.jk.paper_trading_dashboard.order.exception.InvalidOrderException;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

@Service
public class OrderService {

  private final BrokerClient brokerClient;
  private final OrderRepository orderRepository;
  private final BrokerAccountRepository brokerAccountRepository;

  public OrderService(
      BrokerClient brokerClient,
      OrderRepository orderRepository,
      BrokerAccountRepository brokerAccountRepository) {
    this.brokerClient = brokerClient;
    this.orderRepository = orderRepository;
    this.brokerAccountRepository = brokerAccountRepository;
  }

  @Transactional
  public OrderResponse placeOrder(UUID userId, PlaceOrderRequest request) {
    Order order = createPendingOrder(userId, request);

    try {
      BrokerOrderResponse brokerResponse = brokerClient.placeOrder(
          BrokerOrderRequest.from(order));

      order.markSubmitted(
          brokerResponse.brokerOrderId(),
          brokerResponse.brokerStatus());

    } catch (BrokerOrderRejectedException ex) {
      order.markRejected(ex.getMessage(), ex.getBrokerStatus());
    }

    orderRepository.save(order);

    return OrderResponse.from(order);
  }

  public List<OrderResponse> getOrders(UUID userId) {
    return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
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

    if (order.getBrokerOrderId() != null && !order.getBrokerOrderId().isBlank()) {
      brokerClient.cancelOrder(order.getBrokerOrderId());
    }

    order.markCanceled("canceled");
    return OrderResponse.from(order);
  }

  private Order createPendingOrder(UUID userId, PlaceOrderRequest request) {
    UUID brokerAccountId = getBrokerAccountId(userId);

    if (request.type() == OrderType.MARKET) {
      if (request.limitPrice() != null) {
        throw new InvalidOrderException("Market order cannot have limit price");
      }

      return Order.pendingMarketOrder(
          userId,
          brokerAccountId,
          request.symbol(),
          request.side(),
          request.quantity());
    }

    if (request.type() == OrderType.LIMIT) {
      if (request.limitPrice() == null) {
        throw new InvalidOrderException("Limit order requires limit price");
      }

      return Order.pendingLimitOrder(
          userId,
          brokerAccountId,
          request.symbol(),
          request.side(),
          request.quantity(),
          request.limitPrice());
    }

    throw new InvalidOrderException("Unsupported order type");
  }

  private UUID getBrokerAccountId(UUID userId) {
    return brokerAccountRepository.findByUserIdAndActiveTrue(userId)
        .orElseThrow(() -> new NotFoundException("Active broker account not found"))
        .getId();
  }

  private Order getUserOrder(UUID userId, UUID orderId) {
    return orderRepository.findByIdAndUserId(orderId, userId)
        .orElseThrow(() -> new NotFoundException("Order not found"));
  }

  private boolean isCancelable(OrderStatus status) {
    return switch (status) {
      case PENDING, SUBMITTED, ACCEPTED, PARTIALLY_FILLED -> true;
      case FILLED, REJECTED, CANCELED, FAILED -> false;
    };
  }

}
