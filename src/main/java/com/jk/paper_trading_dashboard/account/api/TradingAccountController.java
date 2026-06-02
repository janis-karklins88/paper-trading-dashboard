package com.jk.paper_trading_dashboard.account.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jk.paper_trading_dashboard.account.domain.AccountEquityTimeframe;
import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.dto.AccountEquitySnapshotResponse;
import com.jk.paper_trading_dashboard.account.dto.TradingAccountResponse;
import com.jk.paper_trading_dashboard.account.service.AccountEquitySnapshotService;
import com.jk.paper_trading_dashboard.account.service.TradingAccountService;
import com.jk.paper_trading_dashboard.shared.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/trading-account")
@RequiredArgsConstructor
public class TradingAccountController {

  private final TradingAccountService tradingAccountService;
  private final AccountEquitySnapshotService accountEquitySnapshotService;

  @GetMapping
  public ResponseEntity<TradingAccountResponse> getTradingAccount(@AuthenticationPrincipal UserPrincipal user) {
    return ResponseEntity.ok(tradingAccountService.getAccount(user.userId()));
  }

  @GetMapping("/equity-curve")
  public ResponseEntity<List<AccountEquitySnapshotResponse>> getEquityCurve(
      @AuthenticationPrincipal UserPrincipal user,
      @RequestParam(defaultValue = "ALL") String timeframe) {
    TradingAccount account = tradingAccountService.getActiveAccount(user.userId());
    AccountEquityTimeframe accountEquityTimeframe = AccountEquityTimeframe.fromValue(timeframe);

    return ResponseEntity.ok(accountEquitySnapshotService.getEquityCurve(account, accountEquityTimeframe));
  }

  @PostMapping("/reset")
  public ResponseEntity<TradingAccountResponse> resetTradingAccount(@AuthenticationPrincipal UserPrincipal user) {
    return ResponseEntity.ok(tradingAccountService.resetAccount(user.userId()));
  }
}
