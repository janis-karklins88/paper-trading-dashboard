package com.jk.paper_trading_dashboard.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.dto.TradingAccountResponse;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.service.PriceCacheService;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;
import com.jk.paper_trading_dashboard.user.domain.User;

@ExtendWith(MockitoExtension.class)
class TradingAccountValuationServiceTest {

  @Mock
  private PositionRepository positionRepository;

  @Mock
  private PriceCacheService priceCacheService;

  private TradingAccountValuationService service;

  @BeforeEach
  void setUp() {
    service = new TradingAccountValuationService(positionRepository, priceCacheService);
  }

  @Test
  void toResponseUsesCachedPricesForLiveUnrealizedPnlEquityAndBuyingPower() {
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
    when(positionRepository.findByTradingAccountIdAndStatusOrderByOpenedAtDesc(account.getId(), PositionStatus.OPEN))
        .thenReturn(List.of(position));
    when(priceCacheService.get("TSLA"))
        .thenReturn(Optional.of(new MarketPrice("TSLA", new BigDecimal("255"))));

    TradingAccountResponse response = service.getAccountSummary(account);

    assertThat(response.unrealizedPnl()).isEqualByComparingTo("100");
    assertThat(response.equity()).isEqualByComparingTo("100100");
    assertThat(response.netPnl()).isEqualByComparingTo("100");
    assertThat(response.buyingPower()).isEqualByComparingTo("500000");
  }
}
