package com.jk.paper_trading_dashboard.account.ws;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.dto.TradingAccountResponse;
import com.jk.paper_trading_dashboard.account.service.TradingAccountValuationService;
import com.jk.paper_trading_dashboard.shared.ws.WebSocketGateway;
import com.jk.paper_trading_dashboard.shared.ws.WebSocketTopics;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradingAccountPublisher {

  private final WebSocketGateway webSocketGateway;
  private final TradingAccountValuationService tradingAccountValuationService;

  public void publishAccountUpdate(UUID userId, TradingAccount account) {
    publishAccountUpdate(userId, tradingAccountValuationService.getAccountSummary(account));
  }

  public void publishAccountUpdate(UUID userId, TradingAccountResponse account) {
    webSocketGateway.publish(
        WebSocketTopics.portfolio(userId),
        TradingAccountUpdateMessage.from(account));
  }
}
