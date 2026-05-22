package com.jk.paper_trading_dashboard.position.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.repository.TradingAccountRepository;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;
import com.jk.paper_trading_dashboard.position.ws.PositionPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionMarketDataUpdateService {

  private final PositionRepository positionRepository;
  private final PositionPublisher positionPublisher;
  private final TradingAccountRepository tradingAccountRepository;

  public void onPriceRefreshed(MarketPrice marketPrice) {
    positionRepository.findByStatusAndSymbol(PositionStatus.OPEN, marketPrice.symbol())
        .forEach(position -> publishLivePosition(position, marketPrice));
  }

  private void publishLivePosition(Position position, MarketPrice marketPrice) {
    BigDecimal unrealizedPnl = position.calculateUnrealizedPnl(marketPrice.price());

    PositionResponse response = PositionResponse.from(
        position,
        marketPrice.price(),
        unrealizedPnl);

    UUID userId = resolveUserId(position);
    positionPublisher.publishPositionUpdate(userId, response);
  }

  private UUID resolveUserId(Position position) {
    return tradingAccountRepository.findById(position.getTradingAccountId())
        .orElseThrow()
        .getUser()
        .getId();
  }

}
