package com.jk.paper_trading_dashboard.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.position.service.PositionService;
import com.jk.paper_trading_dashboard.position.ws.PositionPublisher;
import com.jk.paper_trading_dashboard.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LimitOrderExecutionService {

  private static final BigDecimal SPREAD_RATE = new BigDecimal("0.0010");
  private static final BigDecimal TWO = new BigDecimal("2");
  private static final int MONEY_SCALE = 8;

  private final OrderRepository orderRepository;
  private final TradingAccountRepository tradingAccountRepository;
  private final PositionService positionService;
  private final PositionPublisher positionPublisher;

  @Transactional
  public void executeLimitOrder(UUID orderId, BigDecimal marketPrice) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));

    BigDecimal executionPrice = applySpread(marketPrice, order.getSide());

    if (!isExecutableLimitOrder(order, executionPrice)) {
      return;
    }

    TradingAccount account = tradingAccountRepository.findById(order.getTradingAccountId())
        .orElseThrow(() -> new NotFoundException("Trading account not found"));

    order.markFilled(executionPrice);

    Position position = positionService.createPositionForAccount(
        account.getId(),
        createPositionRequest(order, marketPrice, executionPrice));
    account.applyPositionOpen(position.getUnrealizedPnl());
    positionPublisher.publishPositionUpdate(account.getUser().getId(), PositionResponse.from(position));
  }

  private boolean isExecutableLimitOrder(Order order, BigDecimal executionPrice) {
    if (executionPrice == null || executionPrice.signum() <= 0) {
      return false;
    }

    if (order.getStatus() != OrderStatus.OPEN || order.getType() != OrderType.LIMIT) {
      return false;
    }

    return switch (order.getSide()) {
      case BUY -> executionPrice.compareTo(order.getLimitPrice()) <= 0;
      case SELL -> executionPrice.compareTo(order.getLimitPrice()) >= 0;
    };
  }

  private CreatePositionRequest createPositionRequest(
      Order order,
      BigDecimal marketPrice,
      BigDecimal executionPrice) {
    return new CreatePositionRequest(
        order.getSymbol(),
        positionSide(order.getSide()),
        order.getQuantity(),
        executionPrice,
        marketPrice,
        order.getMarginAmount(),
        order.getLeverage());
  }

  private BigDecimal applySpread(BigDecimal marketPrice, OrderSide side) {
    if (marketPrice == null || marketPrice.signum() <= 0) {
      return marketPrice;
    }

    BigDecimal halfSpreadRate = SPREAD_RATE.divide(TWO, MONEY_SCALE, RoundingMode.HALF_UP);
    BigDecimal multiplier = switch (side) {
      case BUY -> BigDecimal.ONE.add(halfSpreadRate);
      case SELL -> BigDecimal.ONE.subtract(halfSpreadRate);
    };

    return marketPrice.multiply(multiplier).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }

  private PositionSide positionSide(OrderSide orderSide) {
    return switch (orderSide) {
      case BUY -> PositionSide.LONG;
      case SELL -> PositionSide.SHORT;
    };
  }

}
