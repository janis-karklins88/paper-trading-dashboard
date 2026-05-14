package com.jk.paper_trading_dashboard.order.scheduler;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

import com.jk.paper_trading_dashboard.alpaca.domain.MarketDataClient;
import com.jk.paper_trading_dashboard.alpaca.domain.MarketPrice;
import com.jk.paper_trading_dashboard.order.domain.Order;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.domain.OrderType;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.order.service.LimitOrderExecutionService;

@Component
public class LimitOrderExecutionJob {

  private final LimitOrderExecutionService limitOrderExecutionService;
  private final MarketDataClient marketDataClient;
  private final OrderRepository orderRepository;

  public LimitOrderExecutionJob(
      LimitOrderExecutionService limitOrderExecutionService,
      MarketDataClient marketDataClient,
      OrderRepository orderRepository) {
    this.limitOrderExecutionService = limitOrderExecutionService;
    this.marketDataClient = marketDataClient;
    this.orderRepository = orderRepository;
  }

  @Scheduled(fixedDelayString = "${app.orders.limit-check-delay-ms:500}")
  public void executeLimitOrders() {
    var openLimitOrders = orderRepository.findByStatusAndType(OrderStatus.OPEN, OrderType.LIMIT);
    for (var order : openLimitOrders) {
      MarketPrice marketPrice = marketDataClient.getLatestPrice(order.getSymbol());

      if (marketPrice == null || marketPrice.price() == null) {
        continue;
      }

      if (isFillable(order, marketPrice)) {
        limitOrderExecutionService.executeLimitOrder(order.getId(), marketPrice.price());
      }
    }
  }

  private boolean isFillable(Order order, MarketPrice marketPrice) {
    return switch (order.getSide()) {
      case BUY -> marketPrice.price().compareTo(order.getLimitPrice()) <= 0;
      case SELL -> marketPrice.price().compareTo(order.getLimitPrice()) >= 0;
    };
  }

}
