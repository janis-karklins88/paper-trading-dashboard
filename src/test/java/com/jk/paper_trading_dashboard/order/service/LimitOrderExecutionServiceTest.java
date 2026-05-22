package com.jk.paper_trading_dashboard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.repository.TradingAccountRepository;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderSide;
import com.jk.paper_trading_dashboard.order.dto.OrderResponse;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.order.ws.OrderPublisher;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.dto.CreatePositionRequest;
import com.jk.paper_trading_dashboard.position.service.PositionService;
import com.jk.paper_trading_dashboard.position.ws.PositionPublisher;
import com.jk.paper_trading_dashboard.user.domain.User;

@ExtendWith(MockitoExtension.class)
class LimitOrderExecutionServiceTest {

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private TradingAccountRepository tradingAccountRepository;

  @Mock
  private PositionService positionService;

  @Mock
  private PositionPublisher positionPublisher;

  @Mock
  private OrderPublisher orderPublisher;

  private LimitOrderExecutionService limitOrderExecutionService;
  private TradingAccount account;

  @BeforeEach
  void setUp() {
    limitOrderExecutionService = new LimitOrderExecutionService(
        orderRepository,
        tradingAccountRepository,
        positionService,
        positionPublisher,
        orderPublisher);
    account = new TradingAccount(new User("test@example.com", "hash"));
  }

  @Test
  void buyLimitOrderExecutesOnlyWhenSpreadAdjustedPriceIsAtOrBelowLimit() {
    Order order = openLimitOrder(OrderSide.BUY, new BigDecimal("100"));
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    limitOrderExecutionService.executeLimitOrder(order.getId(), new BigDecimal("99.96"));

    assertThat(order.getFilledPrice()).isNull();
    verify(positionService, never()).createPositionForAccount(any(), any());
  }

  @Test
  void buyLimitOrderFillsAtSpreadAdjustedPriceAndCreatesLongPosition() {
    Order order = openLimitOrder(OrderSide.BUY, new BigDecimal("100"));
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
    when(tradingAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
    when(positionService.createPositionForAccount(any(), any(CreatePositionRequest.class)))
        .thenAnswer(invocation -> {
          UUID tradingAccountId = invocation.getArgument(0);
          CreatePositionRequest request = invocation.getArgument(1);
          return new Position(
              tradingAccountId,
              request.symbol(),
              request.side(),
              request.quantity(),
              request.avgEntryPrice(),
              request.currentPrice(),
              request.marginUsed(),
              request.leverage());
        });

    limitOrderExecutionService.executeLimitOrder(order.getId(), new BigDecimal("99.95"));

    assertThat(order.getFilledPrice()).isEqualByComparingTo("99.99997500");

    ArgumentCaptor<CreatePositionRequest> captor = ArgumentCaptor.forClass(CreatePositionRequest.class);
    verify(positionService).createPositionForAccount(any(), captor.capture());
    assertThat(captor.getValue().side()).isEqualTo(PositionSide.LONG);
    assertThat(captor.getValue().avgEntryPrice()).isEqualByComparingTo("99.99997500");
    assertThat(captor.getValue().currentPrice()).isEqualByComparingTo("99.95");
    verify(orderPublisher).publishOrderUpdate(any(), any(OrderResponse.class));
  }

  @Test
  void sellLimitOrderFillsAtSpreadAdjustedPriceAndCreatesShortPosition() {
    Order order = openLimitOrder(OrderSide.SELL, new BigDecimal("100"));
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
    when(tradingAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
    when(positionService.createPositionForAccount(any(), any(CreatePositionRequest.class)))
        .thenAnswer(invocation -> {
          UUID tradingAccountId = invocation.getArgument(0);
          CreatePositionRequest request = invocation.getArgument(1);
          return new Position(
              tradingAccountId,
              request.symbol(),
              request.side(),
              request.quantity(),
              request.avgEntryPrice(),
              request.currentPrice(),
              request.marginUsed(),
              request.leverage());
        });

    limitOrderExecutionService.executeLimitOrder(order.getId(), new BigDecimal("100.06"));

    assertThat(order.getFilledPrice()).isEqualByComparingTo("100.00997000");

    ArgumentCaptor<CreatePositionRequest> captor = ArgumentCaptor.forClass(CreatePositionRequest.class);
    verify(positionService).createPositionForAccount(any(), captor.capture());
    assertThat(captor.getValue().side()).isEqualTo(PositionSide.SHORT);
    assertThat(captor.getValue().avgEntryPrice()).isEqualByComparingTo("100.00997000");
    assertThat(captor.getValue().currentPrice()).isEqualByComparingTo("100.06");
    verify(orderPublisher).publishOrderUpdate(any(), any(OrderResponse.class));
  }

  private Order openLimitOrder(OrderSide side, BigDecimal limitPrice) {
    Order order = Order.pendingLimitOrder(
        account.getId(),
        "BTC/USD",
        side,
        new BigDecimal("1000"),
        new BigDecimal("5"),
        limitPrice,
        null,
        null);
    order.markOpen();
    return order;
  }
}
