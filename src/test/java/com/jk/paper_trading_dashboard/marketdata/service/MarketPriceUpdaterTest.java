package com.jk.paper_trading_dashboard.marketdata.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.domain.OrderType;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;
import com.jk.paper_trading_dashboard.watchlist.repository.WatchlistItemRepository;

@ExtendWith(MockitoExtension.class)
class MarketPriceUpdaterTest {

  @Mock
  private PositionRepository positionRepository;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private WatchlistItemRepository watchlistItemRepository;

  @Mock
  private ActiveMarketSymbolService activeMarketSymbolService;

  @Mock
  private MarketPriceService marketPriceService;

  private MarketPriceUpdater marketPriceUpdater;

  @BeforeEach
  void setUp() {
    marketPriceUpdater = new MarketPriceUpdater(
        positionRepository,
        orderRepository,
        watchlistItemRepository,
        activeMarketSymbolService,
        marketPriceService);
  }

  @Test
  void updateMarketPricesRefreshesActiveTradingSymbols() {
    Position position = mock(Position.class);
    when(position.getSymbol()).thenReturn("aapl");

    Order order = mock(Order.class);
    when(order.getSymbol()).thenReturn("btc/usd");

    when(positionRepository.findByStatus(PositionStatus.OPEN)).thenReturn(List.of(position));
    when(orderRepository.findByStatusAndType(OrderStatus.OPEN, OrderType.LIMIT)).thenReturn(List.of(order));
    when(activeMarketSymbolService.getActiveSymbols()).thenReturn(List.of("msft"));

    marketPriceUpdater.updateMarketPrices();

    verify(marketPriceService).refreshPrice("AAPL");
    verify(marketPriceService).refreshPrice("BTC/USD");
    verify(marketPriceService).refreshPrice("MSFT");
    verify(watchlistItemRepository, never()).findDistinctSymbols();
  }

  @Test
  void updateWatchlistMarketPricesRefreshesWatchlistSymbols() {
    when(watchlistItemRepository.findDistinctSymbols()).thenReturn(List.of("btc/usd", "AAPL"));

    marketPriceUpdater.updateWatchlistMarketPrices();

    verify(marketPriceService).refreshPrice("BTC/USD");
    verify(marketPriceService).refreshPrice("AAPL");
    verify(positionRepository, never()).findByStatus(PositionStatus.OPEN);
    verify(orderRepository, never()).findByStatusAndType(OrderStatus.OPEN, OrderType.LIMIT);
    verify(activeMarketSymbolService, never()).getActiveSymbols();
  }
}
