package com.jk.paper_trading_dashboard.account.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.domain.TradingAccountStatus;
import com.jk.paper_trading_dashboard.account.repository.TradingAccountRepository;
import com.jk.paper_trading_dashboard.shared.exception.AlreadyExistsException;
import com.jk.paper_trading_dashboard.shared.exception.NotFoundException;
import com.jk.paper_trading_dashboard.user.domain.User;

import jakarta.transaction.Transactional;

@Service
public class TradingAccountService {

  private final TradingAccountRepository tradingAccountRepository;

  public TradingAccountService(TradingAccountRepository tradingAccountRepository) {
    this.tradingAccountRepository = tradingAccountRepository;
  }

  @Transactional
  public TradingAccount createForUser(User user) {
    if (tradingAccountRepository.existsByUser_Id(user.getId())) {
      throw new AlreadyExistsException("Trading account already exists for user");
    }

    return tradingAccountRepository.save(new TradingAccount(user));
  }

  public TradingAccount getActiveAccount(UUID userId) {
    return tradingAccountRepository.findByUser_IdAndStatus(userId, TradingAccountStatus.ACTIVE)
        .orElseThrow(() -> new NotFoundException("Active trading account not found"));
  }
}
