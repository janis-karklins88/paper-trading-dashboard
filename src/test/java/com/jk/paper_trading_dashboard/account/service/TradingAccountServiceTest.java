package com.jk.paper_trading_dashboard.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.domain.TradingAccountStatus;
import com.jk.paper_trading_dashboard.account.dto.TradingAccountResponse;
import com.jk.paper_trading_dashboard.account.repository.TradingAccountRepository;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.service.MarketPriceService;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;
import com.jk.paper_trading_dashboard.shared.exception.BadRequestException;
import com.jk.paper_trading_dashboard.user.domain.User;

@ExtendWith(MockitoExtension.class)
class TradingAccountServiceTest {

  @Mock
  private TradingAccountRepository tradingAccountRepository;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private PositionRepository positionRepository;

  @Mock
  private MarketPriceService marketPriceService;

  private TradingAccountService tradingAccountService;

  @BeforeEach
  void setUp() {
    tradingAccountService = new TradingAccountService(
        tradingAccountRepository,
        orderRepository,
        positionRepository,
        marketPriceService);
  }

  @Test
  void getAccountUsesCachedPricesForLiveUnrealizedPnlAndEquity() {
    UUID userId = UUID.randomUUID();
    TradingAccount account = new TradingAccount(new User("test@example.com", "hash"));
    Position position = new Position(
        account.getId(),
        "TSLA",
        PositionSide.LONG,
        new BigDecimal("20"),
        new BigDecimal("250"),
        new BigDecimal("250"),
        new BigDecimal("1000"),
        new BigDecimal("5"));
    when(tradingAccountRepository.findByUser_IdAndStatus(userId, TradingAccountStatus.ACTIVE))
        .thenReturn(Optional.of(account));
    when(positionRepository.findByTradingAccountIdAndStatusOrderByOpenedAtDesc(account.getId(), PositionStatus.OPEN))
        .thenReturn(List.of(position));
    when(marketPriceService.getCachedPrice("TSLA"))
        .thenReturn(Optional.of(new MarketPrice("TSLA", new BigDecimal("255"))));

    TradingAccountResponse response = tradingAccountService.getAccount(userId);

    assertThat(response.unrealizedPnl()).isEqualByComparingTo("100");
    assertThat(response.equity()).isEqualByComparingTo("100100");
  }

  @Test
  void resetFailsWhenOpenPositionsExist() {
    UUID userId = UUID.randomUUID();
    TradingAccount account = new TradingAccount(new User("test@example.com", "hash"));
    when(tradingAccountRepository.findByUser_IdAndStatus(userId, TradingAccountStatus.ACTIVE))
        .thenReturn(Optional.of(account));
    when(positionRepository.existsByTradingAccountIdAndStatus(account.getId(), PositionStatus.OPEN))
        .thenReturn(true);

    assertThatThrownBy(() -> tradingAccountService.resetAccount(userId))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("open positions");

    verify(orderRepository, never()).existsByTradingAccountIdAndStatusIn(org.mockito.Mockito.any(), org.mockito.Mockito.any());
  }
}
