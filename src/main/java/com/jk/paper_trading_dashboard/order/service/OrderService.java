package com.jk.paper_trading_dashboard.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.service.AccountEquitySnapshotService;
import com.jk.paper_trading_dashboard.account.service.TradingAccountService;
import com.jk.paper_trading_dashboard.account.ws.TradingAccountPublisher;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.service.MarketPriceService;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderSide;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.dto.OrderResponse;
import com.jk.paper_trading_dashboard.order.dto.PlaceOrderRequest;
import com.jk.paper_trading_dashboard.order.exception.InvalidOrderException;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.order.ws.OrderPublisher;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.dto.CreatePositionRequest;
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.position.service.PositionService;
import com.jk.paper_trading_dashboard.position.ws.PositionPublisher;
import com.jk.paper_trading_dashboard.shared.dto.PageResponse;
import com.jk.paper_trading_dashboard.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

  private static final BigDecimal MARKET_FEE_RATE = new BigDecimal("0.0005");
  private static final BigDecimal LIMIT_FEE_RATE = new BigDecimal("0.0001");
  private static final BigDecimal SPREAD_RATE = new BigDecimal("0.0010");
  private static final BigDecimal TWO = new BigDecimal("2");
  private static final int MONEY_SCALE = 8;

  private final OrderRepository orderRepository;
  private final TradingAccountService tradingAccountService;
  private final PositionService positionService;
  private final MarketPriceService marketPriceService;
  private final PositionPublisher positionPublisher;
  private final OrderPublisher orderPublisher;
  private final TradingAccountPublisher tradingAccountPublisher;
  private final AccountEquitySnapshotService accountEquitySnapshotService;

  @Transactional
  public OrderResponse placeOrder(UUID userId, PlaceOrderRequest request) {
    TradingAccount account = tradingAccountService.getActiveAccount(userId);

    return switch (request.type()) {
      case MARKET -> placeMarketOrder(userId, account, request);
      case LIMIT -> placeLimitOrder(userId, account, request);
    };
  }

  private OrderResponse placeMarketOrder(UUID userId, TradingAccount account, PlaceOrderRequest request) {
    validateMarketOrder(account, request);

    Order order = Order.pendingMarketOrder(
        account.getId(),
        request.symbol(),
        request.side(),
        request.marginAmount(),
        request.leverage(),
        request.takeProfitPrice(),
        request.stopLossPrice());

    reserveMargin(account, request.marginAmount());

    BigDecimal marketPrice = getMarketPrice(request.symbol());
    BigDecimal executionPrice = applySpread(marketPrice, request.side());
    validateExitPrices(positionSide(request.side()), executionPrice, request.takeProfitPrice(), request.stopLossPrice());
    order.markFilled(executionPrice);

    BigDecimal feeAmount = calculateFee(order, MARKET_FEE_RATE);
    order.applyFee(feeAmount);
    account.deductFee(feeAmount);

    Position position = positionService.createPositionForAccount(
        account.getId(),
        createPositionRequest(request, order, marketPrice, executionPrice));
    account.applyPositionOpen(position.getUnrealizedPnl());
    positionPublisher.publishPositionUpdate(userId, PositionResponse.from(position));

    orderRepository.save(order);

    OrderResponse response = OrderResponse.from(order);
    orderPublisher.publishOrderUpdate(userId, response);
    accountEquitySnapshotService.createSnapshotForAccount(account);
    tradingAccountPublisher.publishAccountUpdate(userId, account);
    return response;
  }

  private OrderResponse placeLimitOrder(UUID userId, TradingAccount account, PlaceOrderRequest request) {
    validateLimitOrder(account, request);

    Order order = Order.pendingLimitOrder(
        account.getId(),
        request.symbol(),
        request.side(),
        request.marginAmount(),
        request.leverage(),
        request.limitPrice(),
        request.takeProfitPrice(),
        request.stopLossPrice());

    reserveMargin(account, request.marginAmount());
    BigDecimal feeAmount = calculateFee(order, LIMIT_FEE_RATE);
    order.applyFee(feeAmount);
    account.deductFee(feeAmount);

    order.markOpen();
    orderRepository.save(order);

    OrderResponse response = OrderResponse.from(order);
    orderPublisher.publishOrderUpdate(userId, response);
    accountEquitySnapshotService.createSnapshotForAccount(account);
    tradingAccountPublisher.publishAccountUpdate(userId, account);
    return response;
  }

  private void validateMarketOrder(TradingAccount account, PlaceOrderRequest request) {
    validateOrder(account, request);

    if (request.limitPrice() != null) {
      throw new InvalidOrderException("Market order cannot have limit price");
    }

    BigDecimal estimatedFee = request.marginAmount()
        .multiply(request.leverage())
        .multiply(MARKET_FEE_RATE)
        .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    BigDecimal requiredCash = request.marginAmount().add(estimatedFee);

    if (account.getCashBalance().compareTo(requiredCash) < 0) {
      throw new InvalidOrderException("Insufficient cash balance for margin and fee");
    }
  }

  private void validateLimitOrder(TradingAccount account, PlaceOrderRequest request) {
    validateOrder(account, request);

    if (request.limitPrice() == null) {
      throw new InvalidOrderException("Limit order requires limit price");
    }

    validateExitPrices(positionSide(request.side()), request.limitPrice(), request.takeProfitPrice(), request.stopLossPrice());

    BigDecimal estimatedFee = request.marginAmount()
        .multiply(request.leverage())
        .multiply(LIMIT_FEE_RATE)
        .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    BigDecimal requiredCash = request.marginAmount().add(estimatedFee);

    if (account.getCashBalance().compareTo(requiredCash) < 0) {
      throw new InvalidOrderException("Insufficient cash balance for margin and fee");
    }
  }

  private void validateOrder(TradingAccount account, PlaceOrderRequest request) {
    if (request.leverage().compareTo(account.getMaxLeverage()) > 0) {
      throw new InvalidOrderException("Leverage cannot exceed account max leverage");
    }

    if (account.getCashBalance().compareTo(request.marginAmount()) < 0) {
      throw new InvalidOrderException("Insufficient cash balance");
    }
  }

  private void reserveMargin(TradingAccount account, BigDecimal marginAmount) {
    account.reserveMargin(marginAmount);
  }

  private void validateExitPrices(
      PositionSide side,
      BigDecimal entryPrice,
      BigDecimal takeProfitPrice,
      BigDecimal stopLossPrice) {
    if (side == PositionSide.LONG) {
      if (takeProfitPrice != null && takeProfitPrice.compareTo(entryPrice) <= 0) {
        throw new InvalidOrderException("Take profit must be greater than entry price for long orders");
      }

      if (stopLossPrice != null && stopLossPrice.compareTo(entryPrice) >= 0) {
        throw new InvalidOrderException("Stop loss must be less than entry price for long orders");
      }

      return;
    }

    if (takeProfitPrice != null && takeProfitPrice.compareTo(entryPrice) >= 0) {
      throw new InvalidOrderException("Take profit must be less than entry price for short orders");
    }

    if (stopLossPrice != null && stopLossPrice.compareTo(entryPrice) <= 0) {
      throw new InvalidOrderException("Stop loss must be greater than entry price for short orders");
    }
  }

  private BigDecimal getMarketPrice(String symbol) {
    MarketPrice marketPrice = marketPriceService.refreshPrice(symbol);

    if (marketPrice == null || marketPrice.price() == null || marketPrice.price().signum() <= 0) {
      throw new InvalidOrderException("Market price is unavailable");
    }

    return marketPrice.price();
  }

  private CreatePositionRequest createPositionRequest(
      PlaceOrderRequest request,
      Order order,
      BigDecimal marketPrice,
      BigDecimal executionPrice) {
    return new CreatePositionRequest(
        request.symbol(),
        positionSide(request.side()),
        order.getQuantity(),
        executionPrice,
        marketPrice,
        request.marginAmount(),
        request.leverage(),
        request.takeProfitPrice(),
        request.stopLossPrice());
  }

  private BigDecimal applySpread(BigDecimal marketPrice, OrderSide side) {
    BigDecimal halfSpreadRate = SPREAD_RATE.divide(TWO, MONEY_SCALE, RoundingMode.HALF_UP);
    BigDecimal multiplier = switch (side) {
      case BUY -> BigDecimal.ONE.add(halfSpreadRate);
      case SELL -> BigDecimal.ONE.subtract(halfSpreadRate);
    };

    return marketPrice.multiply(multiplier).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal calculateFee(Order order, BigDecimal feeRate) {
    return order.getNotionalValue().multiply(feeRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }

  private PositionSide positionSide(OrderSide orderSide) {
    return switch (orderSide) {
      case BUY -> PositionSide.LONG;
      case SELL -> PositionSide.SHORT;
    };
  }

  public List<OrderResponse> getOrders(UUID userId) {
    UUID tradingAccountId = getTradingAccountId(userId);

    return orderRepository.findByTradingAccountIdOrderByCreatedAtDesc(tradingAccountId)
        .stream()
        .map(OrderResponse::from)
        .toList();
  }

  public PageResponse<OrderResponse> getOrders(UUID userId, int page, int size) {
    UUID tradingAccountId = getTradingAccountId(userId);
    Pageable pageable = pageRequest(page, size);

    return PageResponse.from(orderRepository.findByTradingAccountIdOrderByCreatedAtDesc(tradingAccountId, pageable)
        .map(OrderResponse::from));
  }

  public OrderResponse getOrder(UUID userId, UUID orderId) {
    Order order = getUserOrder(userId, orderId);
    return OrderResponse.from(order);
  }

  @Transactional
  public OrderResponse cancelOrder(UUID userId, UUID orderId) {
    TradingAccount account = tradingAccountService.getActiveAccount(userId);
    Order order = getAccountOrder(account.getId(), orderId);

    if (!isCancelable(order.getStatus())) {
      throw new InvalidOrderException("Order cannot be canceled from status " + order.getStatus());
    }

    boolean releasedMargin = order.getStatus() == OrderStatus.OPEN;
    if (releasedMargin) {
      account.releaseMargin(order.getMarginAmount());
    }

    order.markCanceled();
    OrderResponse response = OrderResponse.from(order);
    orderPublisher.publishOrderUpdate(userId, response);
    if (releasedMargin) {
      accountEquitySnapshotService.createSnapshotForAccount(account);
    }
    tradingAccountPublisher.publishAccountUpdate(userId, account);
    return response;
  }

  private UUID getTradingAccountId(UUID userId) {
    return tradingAccountService.getActiveAccount(userId).getId();
  }

  private Order getUserOrder(UUID userId, UUID orderId) {
    UUID tradingAccountId = getTradingAccountId(userId);

    return getAccountOrder(tradingAccountId, orderId);
  }

  private Order getAccountOrder(UUID tradingAccountId, UUID orderId) {
    return orderRepository.findByIdAndTradingAccountId(orderId, tradingAccountId)
        .orElseThrow(() -> new NotFoundException("Order not found"));
  }

  private boolean isCancelable(OrderStatus status) {
    return switch (status) {
      case PENDING, OPEN -> true;
      case FILLED, REJECTED, CANCELED -> false;
    };
  }

  private Pageable pageRequest(int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);

    return PageRequest.of(safePage, safeSize);
  }

}
