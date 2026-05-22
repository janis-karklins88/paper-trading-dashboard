package com.jk.paper_trading_dashboard.position.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.jk.paper_trading_dashboard.account.service.TradingAccountService;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.service.MarketPriceService;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderSide;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.dto.OrderResponse;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.order.ws.OrderPublisher;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.position.dto.UpdatePositionExitPricesRequest;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;
import com.jk.paper_trading_dashboard.position.ws.PositionPublisher;
import com.jk.paper_trading_dashboard.user.domain.User;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

  @Mock
  private PositionRepository positionRepository;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private TradingAccountService tradingAccountService;

  @Mock
  private MarketPriceService marketPriceService;

  @Mock
  private PositionPublisher positionPublisher;

  @Mock
  private OrderPublisher orderPublisher;

  private PositionService positionService;
  private TradingAccount account;
  private UUID userId;

  @BeforeEach
  void setUp() {
    positionService = new PositionService(
        positionRepository,
        orderRepository,
        tradingAccountService,
        marketPriceService,
        positionPublisher,
        orderPublisher);
    account = new TradingAccount(new User("test@example.com", "hash"));
    account.reserveMargin(new BigDecimal("1000"));
    userId = UUID.randomUUID();
    when(tradingAccountService.getActiveAccount(userId)).thenReturn(account);
  }

  @Test
  void closePositionCreatesClosingOrderRealizesPnlAndReleasesMargin() {
    UUID positionId = UUID.randomUUID();
    Position position = new Position(
        account.getId(),
        "TSLA",
        PositionSide.LONG,
        new BigDecimal("20"),
        new BigDecimal("250"),
        new BigDecimal("255"),
        new BigDecimal("1000"),
        new BigDecimal("5"));
    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(positionRepository.findByIdAndTradingAccountId(positionId, account.getId()))
        .thenReturn(Optional.of(position));
    when(marketPriceService.refreshPrice("TSLA")).thenReturn(new MarketPrice("TSLA", new BigDecimal("255")));

    PositionResponse response = positionService.closePosition(userId, positionId);

    assertThat(response.status()).isEqualTo(PositionStatus.CLOSED);
    assertThat(response.realizedPnl()).isEqualByComparingTo("97.45");
    assertThat(account.getReservedMargin()).isEqualByComparingTo("0");
    assertThat(account.getCashBalance()).isEqualByComparingTo("100094.90127500");

    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderRepository).save(orderCaptor.capture());
    assertThat(orderCaptor.getValue().getSide()).isEqualTo(OrderSide.SELL);
    assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.FILLED);
    assertThat(orderCaptor.getValue().getFilledPrice()).isEqualByComparingTo("254.87250000");
    assertThat(orderCaptor.getValue().getFeeAmount()).isEqualByComparingTo("2.54872500");
    verify(orderPublisher).publishOrderUpdate(userId, OrderResponse.from(orderCaptor.getValue()));
    verify(positionPublisher).publishPositionUpdate(userId, response);
  }

  @Test
  void updateExitPricesChangesOpenPositionAndPublishesUpdate() {
    UUID positionId = UUID.randomUUID();
    Position position = new Position(
        account.getId(),
        "TSLA",
        PositionSide.LONG,
        new BigDecimal("20"),
        new BigDecimal("250"),
        new BigDecimal("255"),
        new BigDecimal("1000"),
        new BigDecimal("5"));
    when(positionRepository.findByIdAndTradingAccountId(positionId, account.getId()))
        .thenReturn(Optional.of(position));

    PositionResponse response = positionService.updateExitPrices(
        userId,
        positionId,
        new UpdatePositionExitPricesRequest(new BigDecimal("300"), new BigDecimal("220")));

    assertThat(response.takeProfitPrice()).isEqualByComparingTo("300");
    assertThat(response.stopLossPrice()).isEqualByComparingTo("220");
    verify(positionPublisher).publishPositionUpdate(userId, response);
  }
}
