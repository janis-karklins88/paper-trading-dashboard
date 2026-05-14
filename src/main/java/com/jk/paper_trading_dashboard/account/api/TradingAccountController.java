package com.jk.paper_trading_dashboard.account.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jk.paper_trading_dashboard.account.dto.TradingAccountResponse;
import com.jk.paper_trading_dashboard.account.service.TradingAccountService;
import com.jk.paper_trading_dashboard.shared.security.UserPrincipal;

@RestController
@RequestMapping("/trading-account")
public class TradingAccountController {

  private final TradingAccountService tradingAccountService;

  public TradingAccountController(TradingAccountService tradingAccountService) {
    this.tradingAccountService = tradingAccountService;
  }

  @GetMapping
  public ResponseEntity<TradingAccountResponse> getTradingAccount(@AuthenticationPrincipal UserPrincipal user) {
    return ResponseEntity.ok(tradingAccountService.getAccount(user.userId()));
  }

  @PostMapping("/reset")
  public ResponseEntity<TradingAccountResponse> resetTradingAccount(@AuthenticationPrincipal UserPrincipal user) {
    return ResponseEntity.ok(tradingAccountService.resetAccount(user.userId()));
  }
}
