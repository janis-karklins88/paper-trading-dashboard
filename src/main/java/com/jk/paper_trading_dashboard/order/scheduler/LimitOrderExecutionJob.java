package com.jk.paper_trading_dashboard.order.scheduler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.service.MarketPriceService;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.domain.OrderType;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.order.service.LimitOrderExecutionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LimitOrderExecutionJob {

  private static final Logger log = LoggerFactory.getLogger(LimitOrderExecutionJob.class);

  private final LimitOrderExecutionService limitOrderExecutionService;
  private final MarketPriceService marketPriceService;
  private final OrderRepository orderRepository;

  @Scheduled(fixedDelayString = "${app.orders.limit-check-delay-ms:500}")
  public void executeLimitOrders() {
    var openLimitOrders = orderRepository.findByStatusAndType(OrderStatus.OPEN, OrderType.LIMIT);

    Set<String> symbols = new HashSet<>();
    for (var order : openLimitOrders) {
      addSymbol(symbols, order);
    }

    Map<String, MarketPrice> pricesBySymbol = getPricesBySymbol(symbols);

    for (var order : openLimitOrders) {
      String symbol = normalizeOrderSymbol(order);
      if (symbol == null) {
        continue;
      }

      MarketPrice marketPrice = pricesBySymbol.get(symbol);
      if (marketPrice == null || marketPrice.price() == null) {
        continue;
      }

      if (isFillable(order, marketPrice)) {
        executeOrder(order, marketPrice);
      }
    }
  }

  private void addSymbol(Set<String> symbols, Order order) {
    String symbol = normalizeOrderSymbol(order);
    if (symbol != null) {
      symbols.add(symbol);
    }
  }

  private String normalizeOrderSymbol(Order order) {
    try {
      return normalizeSymbol(order.getSymbol());
    } catch (IllegalArgumentException e) {
      log.warn("Skipping limit order {} with invalid symbol", order.getId(), e);
      return null;
    }
  }

  private Map<String, MarketPrice> getPricesBySymbol(Set<String> symbols) {
    Map<String, MarketPrice> pricesBySymbol = new HashMap<>();

    for (String symbol : symbols) {
      try {
        MarketPrice marketPrice = marketPriceService.getPriceOrRefresh(symbol);

        if (marketPrice != null && marketPrice.price() != null) {
          pricesBySymbol.put(symbol, marketPrice);
        }
      } catch (Exception e) {
        log.warn("Failed to get market price for symbol {}", symbol, e);
      }
    }

    return pricesBySymbol;
  }

  private void executeOrder(Order order, MarketPrice marketPrice) {
    try {
      limitOrderExecutionService.executeLimitOrder(order.getId(), marketPrice.price());
    } catch (Exception e) {
      log.warn("Failed to execute limit order {}", order.getId(), e);
    }
  }

  private boolean isFillable(Order order, MarketPrice marketPrice) {
    return switch (order.getSide()) {
      case BUY -> marketPrice.price().compareTo(order.getLimitPrice()) <= 0;
      case SELL -> marketPrice.price().compareTo(order.getLimitPrice()) >= 0;
    };
  }

  private String normalizeSymbol(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("symbol is required");
    }

    return symbol.trim().toUpperCase();
  }

}
