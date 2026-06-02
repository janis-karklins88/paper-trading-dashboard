package com.jk.paper_trading_dashboard.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.jk.paper_trading_dashboard.account.ws.TradingAccountPublisher;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
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
  private TradingAccountValuationService tradingAccountValuationService;

  @Mock
  private TradingAccountPublisher tradingAccountPublisher;

  @Mock
  private AccountEquitySnapshotService accountEquitySnapshotService;

  private TradingAccountService tradingAccountService;

  @BeforeEach
  void setUp() {
    tradingAccountService = new TradingAccountService(
        tradingAccountRepository,
        orderRepository,
        positionRepository,
        tradingAccountValuationService,
        tradingAccountPublisher,
        accountEquitySnapshotService);
  }

  @Test
  void getAccountReturnsLiveValuation() {
    UUID userId = UUID.randomUUID();
    TradingAccount account = new TradingAccount(new User("test@example.com", "hash"));
    TradingAccountResponse expected = TradingAccountResponse.from(account, new BigDecimal("100"));
    when(tradingAccountRepository.findByUser_IdAndStatus(userId, TradingAccountStatus.ACTIVE))
        .thenReturn(Optional.of(account));
    when(tradingAccountValuationService.getAccountSummary(account)).thenReturn(expected);

    TradingAccountResponse response = tradingAccountService.getAccount(userId);

    assertThat(response).isEqualTo(expected);
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
