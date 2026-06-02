package com.jk.paper_trading_dashboard.account.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.dto.TradingAccountResponse;
import com.jk.paper_trading_dashboard.marketdata.service.PriceCacheService;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradingAccountValuationService {

  private final PositionRepository positionRepository;
  private final PriceCacheService priceCacheService;

  public TradingAccountResponse getAccountSummary(TradingAccount account) {
    return TradingAccountResponse.from(account, calculateLiveUnrealizedPnl(account.getId()));
  }

  private BigDecimal calculateLiveUnrealizedPnl(UUID tradingAccountId) {
    return positionRepository.findByTradingAccountIdAndStatusOrderByOpenedAtDesc(tradingAccountId, PositionStatus.OPEN)
        .stream()
        .map(this::calculateLiveUnrealizedPnl)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal calculateLiveUnrealizedPnl(Position position) {
    return priceCacheService.get(position.getSymbol())
        .map(marketPrice -> position.calculateUnrealizedPnl(marketPrice.price()))
        .orElse(position.getUnrealizedPnl());
  }
}
