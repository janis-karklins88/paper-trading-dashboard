package com.jk.paper_trading_dashboard.account.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.domain.TradingAccountStatus;
import com.jk.paper_trading_dashboard.account.dto.TradingAccountResponse;
import com.jk.paper_trading_dashboard.account.repository.TradingAccountRepository;
import com.jk.paper_trading_dashboard.order.domain.OrderStatus;
import com.jk.paper_trading_dashboard.order.repository.OrderRepository;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;
import com.jk.paper_trading_dashboard.shared.exception.AlreadyExistsException;
import com.jk.paper_trading_dashboard.shared.exception.BadRequestException;
import com.jk.paper_trading_dashboard.shared.exception.NotFoundException;
import com.jk.paper_trading_dashboard.user.domain.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradingAccountService {

  private static final List<OrderStatus> OPEN_ORDER_STATUSES = List.of(
      OrderStatus.PENDING,
      OrderStatus.OPEN);

  private final TradingAccountRepository tradingAccountRepository;
  private final OrderRepository orderRepository;
  private final PositionRepository positionRepository;

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

  public TradingAccountResponse getAccount(UUID userId) {
    return TradingAccountResponse.from(getActiveAccount(userId));
  }

  @Transactional
  public TradingAccountResponse resetAccount(UUID userId) {
    TradingAccount account = getActiveAccount(userId);

    validateNoOpenPositions(account.getId());
    validateNoOpenOrders(account.getId());

    account.reset();
    return TradingAccountResponse.from(account);
  }

  private void validateNoOpenPositions(UUID tradingAccountId) {
    if (positionRepository.existsByTradingAccountIdAndStatus(tradingAccountId, PositionStatus.OPEN)) {
      throw new BadRequestException("Trading account cannot be reset while open positions exist");
    }
  }

  private void validateNoOpenOrders(UUID tradingAccountId) {
    if (orderRepository.existsByTradingAccountIdAndStatusIn(tradingAccountId, OPEN_ORDER_STATUSES)) {
      throw new BadRequestException("Trading account cannot be reset while open orders exist");
    }
  }
}
