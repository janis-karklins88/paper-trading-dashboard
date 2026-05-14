package com.jk.paper_trading_dashboard.order.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.repository.TradingAccountRepository;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderSide;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.domain.OrderType;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.dto.CreatePositionRequest;
import com.jk.paper_trading_dashboard.position.service.PositionService;
import com.jk.paper_trading_dashboard.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LimitOrderExecutionService {

  private final OrderRepository orderRepository;
  private final TradingAccountRepository tradingAccountRepository;
  private final PositionService positionService;

  @Transactional
  public void executeLimitOrder(UUID orderId, BigDecimal marketPrice) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));

    if (!isExecutableLimitOrder(order, marketPrice)) {
      return;
    }

    TradingAccount account = tradingAccountRepository.findById(order.getTradingAccountId())
        .orElseThrow(() -> new NotFoundException("Trading account not found"));

    order.markFilled(marketPrice);

    Position position = positionService.createPositionForAccount(
        account.getId(),
        createPositionRequest(order, marketPrice));
    account.applyPositionOpen(position.getUnrealizedPnl());
  }

  private boolean isExecutableLimitOrder(Order order, BigDecimal marketPrice) {
    if (marketPrice == null || marketPrice.signum() <= 0) {
      return false;
    }

    if (order.getStatus() != OrderStatus.OPEN || order.getType() != OrderType.LIMIT) {
      return false;
    }

    return switch (order.getSide()) {
      case BUY -> marketPrice.compareTo(order.getLimitPrice()) <= 0;
      case SELL -> marketPrice.compareTo(order.getLimitPrice()) >= 0;
    };
  }

  private CreatePositionRequest createPositionRequest(Order order, BigDecimal executionPrice) {
    return new CreatePositionRequest(
        order.getSymbol(),
        positionSide(order.getSide()),
        order.getQuantity(),
        executionPrice,
        executionPrice,
        order.getMarginAmount(),
        order.getLeverage());
  }

  private PositionSide positionSide(OrderSide orderSide) {
    return switch (orderSide) {
      case BUY -> PositionSide.LONG;
      case SELL -> PositionSide.SHORT;
    };
  }
  
}
