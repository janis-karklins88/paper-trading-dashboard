package com.jk.paper_trading_dashboard.position.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.jk.paper_trading_dashboard.account.service.TradingAccountService;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.service.MarketPriceService;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionSide;
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
    private TradingAccountService tradingAccountService;

    @Mock
    private MarketPriceService marketPriceService;

    @Mock
    private PositionPublisher positionPublisher;

    @Mock
    private PositionCloseService positionCloseService;

    private PositionService positionService;
    private TradingAccount account;
    private UUID userId;

    @BeforeEach
    void setUp() {
        positionService = new PositionService(
                positionRepository,
                tradingAccountService,
                marketPriceService,
                positionPublisher,
                positionCloseService);
        account = new TradingAccount(new User("test@example.com", "hash"));
        account.reserveMargin(new BigDecimal("1000"));
        userId = UUID.randomUUID();
        when(tradingAccountService.getActiveAccount(userId)).thenReturn(account);
    }

    @Test
    void closePositionDelegatesToCloseServiceWithRefreshedMarketPrice() {
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
        PositionResponse expectedResponse = PositionResponse.from(position);
        when(positionRepository.findByIdAndTradingAccountId(positionId, account.getId()))
                .thenReturn(Optional.of(position));
        when(marketPriceService.refreshPrice("TSLA"))
                .thenReturn(new MarketPrice("TSLA", new BigDecimal("255")));
        when(positionCloseService.closeAtMarketPrice(userId, account, position, new BigDecimal("255")))
                .thenReturn(expectedResponse);

        PositionResponse response = positionService.closePosition(userId, positionId);

        assertThat(response).isEqualTo(expectedResponse);
        verify(positionCloseService).closeAtMarketPrice(userId, account, position, new BigDecimal("255"));
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
        verify(positionCloseService, never()).closeAtMarketPrice(userId, account, position, new BigDecimal("255"));
        verify(positionPublisher).publishPositionUpdate(userId, response);
    }

    @Test
    void updateExitPricesClosesPositionImmediatelyWhenCachedPriceCrossesExitPrice() {
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
        PositionResponse expectedResponse = PositionResponse.from(
                position,
                new BigDecimal("255"),
                position.calculateUnrealizedPnl(new BigDecimal("255")));
        when(positionRepository.findByIdAndTradingAccountId(positionId, account.getId()))
                .thenReturn(Optional.of(position));
        when(marketPriceService.getCachedPrice("TSLA"))
                .thenReturn(Optional.of(new MarketPrice("TSLA", new BigDecimal("255"))));
        when(positionCloseService.closeAtMarketPrice(userId, account, position, new BigDecimal("255")))
                .thenReturn(expectedResponse);

        PositionResponse response = positionService.updateExitPrices(
                userId,
                positionId,
                new UpdatePositionExitPricesRequest(new BigDecimal("252"), new BigDecimal("220")));

        assertThat(response).isEqualTo(expectedResponse);
        verify(positionCloseService).closeAtMarketPrice(userId, account, position, new BigDecimal("255"));
        verify(positionPublisher, never()).publishPositionUpdate(userId, response);
    }
}
