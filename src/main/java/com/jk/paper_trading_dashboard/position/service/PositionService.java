package com.jk.paper_trading_dashboard.position.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.service.TradingAccountService;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.dto.CreatePositionRequest;
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;
import com.jk.paper_trading_dashboard.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

@Service
public class PositionService {

  private final PositionRepository positionRepository;
  private final TradingAccountService tradingAccountService;

  public PositionService(
      PositionRepository positionRepository,
      TradingAccountService tradingAccountService) {
    this.positionRepository = positionRepository;
    this.tradingAccountService = tradingAccountService;
  }

  @Transactional
  public PositionResponse createPosition(UUID userId, CreatePositionRequest request) {
    UUID tradingAccountId = getTradingAccountId(userId);

    Position position = new Position(
        tradingAccountId,
        request.symbol(),
        request.side(),
        request.quantity(),
        request.avgEntryPrice(),
        request.currentPrice(),
        request.marginUsed(),
        request.leverage());

    return PositionResponse.from(positionRepository.save(position));
  }

  public List<PositionResponse> getPositions(UUID userId) {
    UUID tradingAccountId = getTradingAccountId(userId);

    return positionRepository.findByTradingAccountIdOrderByOpenedAtDesc(tradingAccountId)
        .stream()
        .map(PositionResponse::from)
        .toList();
  }

  public PositionResponse getPosition(UUID userId, UUID positionId) {
    UUID tradingAccountId = getTradingAccountId(userId);

    Position position = positionRepository.findByIdAndTradingAccountId(positionId, tradingAccountId)
        .orElseThrow(() -> new NotFoundException("Position not found"));

    return PositionResponse.from(position);
  }

  private UUID getTradingAccountId(UUID userId) {
    return tradingAccountService.getActiveAccount(userId).getId();
  }
}
