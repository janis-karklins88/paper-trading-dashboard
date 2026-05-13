package com.jk.paper_trading_dashboard.position.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.service.TradingAccountService;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderSide;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.dto.CreatePositionRequest;
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;
import com.jk.paper_trading_dashboard.shared.exception.BadRequestException;
import com.jk.paper_trading_dashboard.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

@Service
public class PositionService {

  private final PositionRepository positionRepository;
  private final OrderRepository orderRepository;
  private final TradingAccountService tradingAccountService;

  public PositionService(
      PositionRepository positionRepository,
      OrderRepository orderRepository,
      TradingAccountService tradingAccountService) {
    this.positionRepository = positionRepository;
    this.orderRepository = orderRepository;
    this.tradingAccountService = tradingAccountService;
  }

  @Transactional
  public PositionResponse createPosition(UUID userId, CreatePositionRequest request) {
    UUID tradingAccountId = getTradingAccountId(userId);

    return PositionResponse.from(createPositionForAccount(tradingAccountId, request));
  }

  public Position createPositionForAccount(UUID tradingAccountId, CreatePositionRequest request) {
    Position position = new Position(
        tradingAccountId,
        request.symbol(),
        request.side(),
        request.quantity(),
        request.avgEntryPrice(),
        request.currentPrice(),
        request.marginUsed(),
        request.leverage());

    return positionRepository.save(position);
  }

  public List<PositionResponse> getPositions(UUID userId, PositionStatus status) {
    UUID tradingAccountId = getTradingAccountId(userId);

    List<Position> positions = status == null
        ? positionRepository.findByTradingAccountIdOrderByOpenedAtDesc(tradingAccountId)
        : positionRepository.findByTradingAccountIdAndStatusOrderByOpenedAtDesc(tradingAccountId, status);

    return positions
        .stream()
        .map(PositionResponse::from)
        .toList();
  }

  public PositionResponse getPosition(UUID userId, UUID positionId) {
    UUID tradingAccountId = getTradingAccountId(userId);

    Position position = positionRepository.findByIdAndTradingAccountId(positionId, tradingAccountId)
        .orElseThrow(() -> new NotFoundException("Position not found"));

    return PositionResponse.from(position);
  }

  @Transactional
  public PositionResponse closePosition(UUID userId, UUID positionId) {
    TradingAccount account = tradingAccountService.getActiveAccount(userId);

    Position position = positionRepository.findByIdAndTradingAccountId(positionId, account.getId())
        .orElseThrow(() -> new NotFoundException("Position not found"));

    if (position.getStatus() != PositionStatus.OPEN) {
      throw new BadRequestException("Only open positions can be closed");
    }

    Order closingOrder = Order.pendingMarketOrder(
        account.getId(),
        position.getSymbol(),
        closingSide(position.getSide()),
        closingMarginAmount(position),
        position.getLeverage(),
        null,
        null);
    closingOrder.markFilled(position.getCurrentPrice());
    orderRepository.save(closingOrder);

    BigDecimal realizedPnl = position.close();
    account.applyPositionClose(realizedPnl, position.getMarginUsed());

    return PositionResponse.from(position);
  }

  private OrderSide closingSide(PositionSide positionSide) {
    return switch (positionSide) {
      case LONG -> OrderSide.SELL;
      case SHORT -> OrderSide.BUY;
    };
  }

  private BigDecimal closingMarginAmount(Position position) {
    return position.getQuantity()
        .multiply(position.getCurrentPrice())
        .divide(position.getLeverage(), 8, java.math.RoundingMode.HALF_UP);
  }

  private UUID getTradingAccountId(UUID userId) {
    return tradingAccountService.getActiveAccount(userId).getId();
  }
}
