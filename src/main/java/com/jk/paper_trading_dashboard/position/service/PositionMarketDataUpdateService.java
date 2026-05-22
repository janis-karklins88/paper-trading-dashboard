package com.jk.paper_trading_dashboard.position.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.repository.TradingAccountRepository;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderSide;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;
import com.jk.paper_trading_dashboard.position.ws.PositionPublisher;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionMarketDataUpdateService {

  private static final BigDecimal MARKET_FEE_RATE = new BigDecimal("0.0005");
  private static final BigDecimal SPREAD_RATE = new BigDecimal("0.0010");
  private static final BigDecimal TWO = new BigDecimal("2");
  private static final int MONEY_SCALE = 8;

  private final PositionRepository positionRepository;
  private final OrderRepository orderRepository;
  private final PositionPublisher positionPublisher;
  private final TradingAccountRepository tradingAccountRepository;

  @Transactional
  public void onPriceRefreshed(MarketPrice marketPrice) {
    positionRepository.findByStatusAndSymbol(PositionStatus.OPEN, marketPrice.symbol())
        .forEach(position -> handlePriceUpdate(position, marketPrice));
  }

  private void handlePriceUpdate(Position position, MarketPrice marketPrice) {
    if (position.shouldTakeProfit(marketPrice.price()) || position.shouldStopLoss(marketPrice.price())) {
      closePosition(position, marketPrice.price());
      return;
    }

    publishLivePosition(position, marketPrice);
  }

  private void closePosition(Position position, BigDecimal marketPrice) {
    TradingAccount account = resolveAccount(position);
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

    BigDecimal closedUnrealizedPnl = position.getUnrealizedPnl();
    position.setCurrentPrice(executionPrice);
    BigDecimal realizedPnl = position.close();
    account.applyPositionClose(realizedPnl, position.getMarginUsed(), closedUnrealizedPnl);
    account.deductFee(feeAmount);

    positionPublisher.publishPositionUpdate(account.getUser().getId(), PositionResponse.from(position));
  }

  private void publishLivePosition(Position position, MarketPrice marketPrice) {
    BigDecimal unrealizedPnl = position.calculateUnrealizedPnl(marketPrice.price());

    PositionResponse response = PositionResponse.from(
        position,
        marketPrice.price(),
        unrealizedPnl);

    positionPublisher.publishPositionUpdate(resolveAccount(position).getUser().getId(), response);
  }

  private TradingAccount resolveAccount(Position position) {
    return tradingAccountRepository.findById(position.getTradingAccountId())
        .orElseThrow();
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
