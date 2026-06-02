package com.jk.paper_trading_dashboard.position.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.repository.TradingAccountRepository;
import com.jk.paper_trading_dashboard.account.ws.TradingAccountPublisher;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;
import com.jk.paper_trading_dashboard.position.ws.PositionPublisher;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionMarketDataUpdateService {

  private final PositionRepository positionRepository;
  private final PositionPublisher positionPublisher;
  private final TradingAccountRepository tradingAccountRepository;
  private final PositionCloseService positionCloseService;
  private final TradingAccountPublisher tradingAccountPublisher;

  @Transactional
  public void onPriceRefreshed(MarketPrice marketPrice) {
    positionRepository.findByStatusAndSymbol(PositionStatus.OPEN, marketPrice.symbol())
        .forEach(position -> handlePriceUpdate(position, marketPrice));
  }

  private void handlePriceUpdate(Position position, MarketPrice marketPrice) {
    if (position.shouldTakeProfit(marketPrice.price()) || position.shouldStopLoss(marketPrice.price())) {
      TradingAccount account = resolveAccount(position);
      positionCloseService.closeAtMarketPrice(account.getUser().getId(), account, position, marketPrice.price());
      return;
    }

    publishLivePosition(position, marketPrice);
  }

  private void publishLivePosition(Position position, MarketPrice marketPrice) {
    TradingAccount account = resolveAccount(position);
    BigDecimal unrealizedPnl = position.calculateUnrealizedPnl(marketPrice.price());

    PositionResponse response = PositionResponse.from(
        position,
        marketPrice.price(),
        unrealizedPnl);

    positionPublisher.publishPositionUpdate(account.getUser().getId(), response);
    tradingAccountPublisher.publishAccountUpdate(account.getUser().getId(), account);
  }

  private TradingAccount resolveAccount(Position position) {
    return tradingAccountRepository.findById(position.getTradingAccountId())
        .orElseThrow();
  }

}
