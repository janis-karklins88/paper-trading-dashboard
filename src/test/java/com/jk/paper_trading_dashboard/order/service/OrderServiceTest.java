package com.jk.paper_trading_dashboard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.service.TradingAccountService;
import com.jk.paper_trading_dashboard.alpaca.domain.MarketDataClient;
import com.jk.paper_trading_dashboard.alpaca.domain.MarketPrice;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderSide;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.domain.OrderType;
import com.jk.paper_trading_dashboard.order.dto.OrderResponse;
import com.jk.paper_trading_dashboard.order.dto.PlaceOrderRequest;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.dto.CreatePositionRequest;
import com.jk.paper_trading_dashboard.position.service.PositionService;
import com.jk.paper_trading_dashboard.user.domain.User;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private TradingAccountService tradingAccountService;

  @Mock
  private PositionService positionService;

  @Mock
  private MarketDataClient marketDataClient;

  private OrderService orderService;
  private TradingAccount account;
  private UUID userId;

  @BeforeEach
  void setUp() {
    orderService = new OrderService(orderRepository, tradingAccountService, positionService, marketDataClient);
    account = new TradingAccount(new User("test@example.com", "hash"));
    userId = UUID.randomUUID();
    when(tradingAccountService.getActiveAccount(userId)).thenReturn(account);
    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void marketOrderReservesMarginAppliesSpreadFeeAndCreatesPosition() {
    PlaceOrderRequest request = new PlaceOrderRequest(
        "TSLA",
        OrderSide.BUY,
        OrderType.MARKET,
        new BigDecimal("1000"),
        new BigDecimal("5"),
        null,
        new BigDecimal("300"),
        new BigDecimal("220"));
    when(marketDataClient.getLatestPrice("TSLA")).thenReturn(new MarketPrice("TSLA", new BigDecimal("250")));
    when(positionService.createPositionForAccount(any(), any(CreatePositionRequest.class)))
        .thenAnswer(invocation -> {
          UUID tradingAccountId = invocation.getArgument(0);
          CreatePositionRequest positionRequest = invocation.getArgument(1);
          return new Position(
              tradingAccountId,
              positionRequest.symbol(),
              positionRequest.side(),
              positionRequest.quantity(),
              positionRequest.avgEntryPrice(),
              positionRequest.currentPrice(),
              positionRequest.marginUsed(),
              positionRequest.leverage());
        });

    OrderResponse response = orderService.placeOrder(userId, request);

    assertThat(response.status()).isEqualTo(OrderStatus.FILLED);
    assertThat(response.executionPrice()).isEqualByComparingTo("250.12500000");
    assertThat(response.feeAmount()).isEqualByComparingTo("2.50000000");
    assertThat(response.quantity()).isEqualByComparingTo("19.99000500");
    assertThat(account.getCashBalance()).isEqualByComparingTo("98997.50000000");
    assertThat(account.getReservedMargin()).isEqualByComparingTo("1000");

    ArgumentCaptor<CreatePositionRequest> positionRequestCaptor = ArgumentCaptor.forClass(CreatePositionRequest.class);
    verify(positionService).createPositionForAccount(any(), positionRequestCaptor.capture());
    assertThat(positionRequestCaptor.getValue().side()).isEqualTo(PositionSide.LONG);
    assertThat(positionRequestCaptor.getValue().avgEntryPrice()).isEqualByComparingTo("250.12500000");
    assertThat(positionRequestCaptor.getValue().currentPrice()).isEqualByComparingTo("250");
  }

  @Test
  void limitOrderIsMarkedOpenAndDoesNotCreatePositionOrFetchPrice() {
    PlaceOrderRequest request = new PlaceOrderRequest(
        "TSLA",
        OrderSide.BUY,
        OrderType.LIMIT,
        new BigDecimal("1000"),
        new BigDecimal("5"),
        new BigDecimal("240"),
        null,
        null);

    OrderResponse response = orderService.placeOrder(userId, request);

    assertThat(response.status()).isEqualTo(OrderStatus.OPEN);
    assertThat(response.executionPrice()).isNull();
    assertThat(response.feeAmount()).isEqualByComparingTo("0.50000000");
    assertThat(account.getCashBalance()).isEqualByComparingTo("98999.50000000");
    assertThat(account.getReservedMargin()).isEqualByComparingTo("1000");

    verify(positionService, never()).createPositionForAccount(any(), any());
    verify(marketDataClient, never()).getLatestPrice(any());
  }
}
