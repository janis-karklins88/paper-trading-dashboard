package com.jk.paper_trading_dashboard.position.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.service.AccountEquitySnapshotService;
import com.jk.paper_trading_dashboard.account.ws.TradingAccountPublisher;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderSide;
import com.jk.paper_trading_dashboard.order.dto.OrderResponse;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.order.ws.OrderPublisher;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.position.ws.PositionPublisher;
import com.jk.paper_trading_dashboard.shared.exception.BadRequestException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionCloseService {

  private static final BigDecimal MARKET_FEE_RATE = new BigDecimal("0.0005");
  private static final BigDecimal SPREAD_RATE = new BigDecimal("0.0010");
  private static final BigDecimal TWO = new BigDecimal("2");
  private static final int MONEY_SCALE = 8;

  private final OrderRepository orderRepository;
  private final PositionPublisher positionPublisher;
  private final OrderPublisher orderPublisher;
  private final TradingAccountPublisher tradingAccountPublisher;
  private final AccountEquitySnapshotService accountEquitySnapshotService;

  @Transactional
  public PositionResponse closeAtMarketPrice(
      UUID userId,
      TradingAccount account,
      Position position,
      BigDecimal marketPrice) {
    if (position.getStatus() != PositionStatus.OPEN) {
      throw new BadRequestException("Only open positions can be closed");
    }

    OrderSide closingSide = closingSide(position.getSide());
    BigDecimal executionPrice = applySpread(marketPrice, closingSide);

    Order closingOrder = Order.pendingMarketOrder(
        account.getId(),
        position.getSymbol(),
        closingSide,
        closingMarginAmount(position, executionPrice),
        position.getLeverage(),
        null,
        null);

    closingOrder.markFilled(executionPrice);

    BigDecimal feeAmount = calculateFee(closingOrder);
    closingOrder.applyFee(feeAmount);
    orderRepository.save(closingOrder);
    orderPublisher.publishOrderUpdate(userId, OrderResponse.from(closingOrder));

    BigDecimal closedUnrealizedPnl = position.getUnrealizedPnl();
    position.setCurrentPrice(executionPrice);
    BigDecimal realizedPnl = position.close();
    account.applyPositionClose(realizedPnl, position.getMarginUsed(), closedUnrealizedPnl);
    account.deductFee(feeAmount);

    PositionResponse response = PositionResponse.from(position);
    positionPublisher.publishPositionUpdate(userId, response);
    accountEquitySnapshotService.createSnapshotForAccount(account);
    tradingAccountPublisher.publishAccountUpdate(userId, account);
    return response;
  }

  private OrderSide closingSide(PositionSide positionSide) {
    return switch (positionSide) {
      case LONG -> OrderSide.SELL;
      case SHORT -> OrderSide.BUY;
    };
  }

  private BigDecimal closingMarginAmount(Position position, BigDecimal executionPrice) {
    return position.getQuantity()
        .multiply(executionPrice)
        .divide(position.getLeverage(), MONEY_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal applySpread(BigDecimal marketPrice, OrderSide side) {
    BigDecimal halfSpreadRate = SPREAD_RATE.divide(TWO, MONEY_SCALE, RoundingMode.HALF_UP);
    BigDecimal multiplier = switch (side) {
      case BUY -> BigDecimal.ONE.add(halfSpreadRate);
      case SELL -> BigDecimal.ONE.subtract(halfSpreadRate);
    };

    return marketPrice.multiply(multiplier).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal calculateFee(Order order) {
    return order.getNotionalValue().multiply(MARKET_FEE_RATE).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }
}
