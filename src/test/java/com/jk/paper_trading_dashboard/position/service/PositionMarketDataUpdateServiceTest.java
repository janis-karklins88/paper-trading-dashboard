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
  private PositionPublisher positionPublisher;

  @Mock
  private TradingAccountRepository tradingAccountRepository;

  @Mock
  private PositionCloseService positionCloseService;

  private PositionMarketDataUpdateService service;
  private TradingAccount account;

  @BeforeEach
  void setUp() {
    service = new PositionMarketDataUpdateService(
        positionRepository,
        positionPublisher,
        tradingAccountRepository,
        positionCloseService);
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
    verify(positionCloseService, never()).closeAtMarketPrice(any(), any(), any(), any());
    assertThat(responseCaptor.getValue().status()).isEqualTo(PositionStatus.OPEN);
    assertThat(responseCaptor.getValue().currentPrice()).isEqualByComparingTo("105");
    assertThat(responseCaptor.getValue().unrealizedPnl()).isEqualByComparingTo("50");
  }

  @Test
  void priceRefreshClosesLongPositionWhenTakeProfitIsHit() {
    Position position = positionWithExits();
    when(positionRepository.findByStatusAndSymbol(PositionStatus.OPEN, "TSLA"))
        .thenReturn(List.of(position));
    when(tradingAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

    service.onPriceRefreshed(new MarketPrice("TSLA", new BigDecimal("111")));

    verify(positionCloseService).closeAtMarketPrice(
        account.getUser().getId(),
        account,
        position,
        new BigDecimal("111"));
    verify(positionPublisher, never()).publishPositionUpdate(any(), any());
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
