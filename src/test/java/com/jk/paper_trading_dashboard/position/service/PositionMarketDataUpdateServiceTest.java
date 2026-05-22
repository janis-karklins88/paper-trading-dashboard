package com.jk.paper_trading_dashboard.position.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.repository.TradingAccountRepository;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderSide;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;
import com.jk.paper_trading_dashboard.position.ws.PositionPublisher;
import com.jk.paper_trading_dashboard.user.domain.User;

@ExtendWith(MockitoExtension.class)
class PositionMarketDataUpdateServiceTest {

  @Mock
  private PositionRepository positionRepository;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private PositionPublisher positionPublisher;

  @Mock
  private TradingAccountRepository tradingAccountRepository;

  private PositionMarketDataUpdateService service;
  private TradingAccount account;

  @BeforeEach
  void setUp() {
    service = new PositionMarketDataUpdateService(
        positionRepository,
        orderRepository,
        positionPublisher,
        tradingAccountRepository);
    account = new TradingAccount(new User("test@example.com", "hash"));
  }

  @Test
  void priceRefreshPublishesLivePositionWhenExitPricesAreNotHit() {
    Position position = positionWithExits();
    when(positionRepository.findByStatusAndSymbol(PositionStatus.OPEN, "TSLA"))
        .thenReturn(List.of(position));
    when(tradingAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

    service.onPriceRefreshed(new MarketPrice("TSLA", new BigDecimal("105")));

    ArgumentCaptor<PositionResponse> responseCaptor = ArgumentCaptor.forClass(PositionResponse.class);
    verify(positionPublisher).publishPositionUpdate(any(), responseCaptor.capture());
    verify(orderRepository, never()).save(any());
    assertThat(responseCaptor.getValue().status()).isEqualTo(PositionStatus.OPEN);
    assertThat(responseCaptor.getValue().currentPrice()).isEqualByComparingTo("105");
    assertThat(responseCaptor.getValue().unrealizedPnl()).isEqualByComparingTo("50");
  }

  @Test
  void priceRefreshClosesLongPositionWhenTakeProfitIsHit() {
    account.reserveMargin(new BigDecimal("100"));
    Position position = positionWithExits();
    when(positionRepository.findByStatusAndSymbol(PositionStatus.OPEN, "TSLA"))
        .thenReturn(List.of(position));
    when(tradingAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.onPriceRefreshed(new MarketPrice("TSLA", new BigDecimal("111")));

    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    ArgumentCaptor<PositionResponse> responseCaptor = ArgumentCaptor.forClass(PositionResponse.class);
    verify(orderRepository).save(orderCaptor.capture());
    verify(positionPublisher).publishPositionUpdate(any(), responseCaptor.capture());

    assertThat(orderCaptor.getValue().getSide()).isEqualTo(OrderSide.SELL);
    assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.FILLED);
    assertThat(orderCaptor.getValue().getFilledPrice()).isEqualByComparingTo("110.94450000");
    assertThat(orderCaptor.getValue().getFeeAmount()).isEqualByComparingTo("0.55472250");

    assertThat(responseCaptor.getValue().status()).isEqualTo(PositionStatus.CLOSED);
    assertThat(responseCaptor.getValue().currentPrice()).isEqualByComparingTo("110.94450000");
    assertThat(responseCaptor.getValue().realizedPnl()).isEqualByComparingTo("109.44500000");
    assertThat(account.getReservedMargin()).isEqualByComparingTo("0");
    assertThat(account.getCashBalance()).isEqualByComparingTo("100108.89027750");
  }

  private Position positionWithExits() {
    return new Position(
        account.getId(),
        "TSLA",
        PositionSide.LONG,
        new BigDecimal("10"),
        new BigDecimal("100"),
        new BigDecimal("100"),
        new BigDecimal("100"),
        new BigDecimal("10"),
        new BigDecimal("110"),
        new BigDecimal("90"));
  }
}
