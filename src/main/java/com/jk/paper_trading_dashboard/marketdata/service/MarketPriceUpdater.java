package com.jk.paper_trading_dashboard.marketdata.service;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.marketdata.domain.Symbols;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.domain.OrderType;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketPriceUpdater {

  private static final Logger log = LoggerFactory.getLogger(MarketPriceUpdater.class);

  private final PositionRepository positionRepository;
  private final OrderRepository orderRepository;
  private final MarketPriceService marketPriceService;

  public void updateMarketPrices() {
    var symbolsToTrack = getSymbolsToTrack();

    for (var symbol : symbolsToTrack) {
      try {
        marketPriceService.refreshPrice(symbol);
      } catch (Exception e) {
        log.warn("Failed to refresh price for symbol {}", symbol, e);
      }
    }
  }

  private Set<String> getSymbolsToTrack() {
    Set<String> symbols = new HashSet<>();

    positionRepository.findByStatus(PositionStatus.OPEN)
        .stream()
        .map(Position::getSymbol)
        .map(Symbols::normalize)
        .forEach(symbols::add);

    orderRepository.findByStatusAndType(OrderStatus.OPEN, OrderType.LIMIT)
        .stream()
        .map(Order::getSymbol)
        .map(Symbols::normalize)
        .forEach(symbols::add);

    return symbols;
  }
}
