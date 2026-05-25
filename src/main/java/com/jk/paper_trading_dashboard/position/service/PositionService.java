package com.jk.paper_trading_dashboard.position.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.account.domain.TradingAccount;
import com.jk.paper_trading_dashboard.account.service.TradingAccountService;
import com.jk.paper_trading_dashboard.marketdata.domain.MarketPrice;
import com.jk.paper_trading_dashboard.marketdata.service.MarketPriceService;
import com.jk.paper_trading_dashboard.position.domain.Position;
import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.dto.CreatePositionRequest;
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.position.dto.UpdatePositionExitPricesRequest;
import com.jk.paper_trading_dashboard.position.repository.PositionRepository;
import com.jk.paper_trading_dashboard.position.ws.PositionPublisher;
import com.jk.paper_trading_dashboard.shared.dto.PageResponse;
import com.jk.paper_trading_dashboard.shared.exception.BadRequestException;
import com.jk.paper_trading_dashboard.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionService {

  private final PositionRepository positionRepository;
  private final TradingAccountService tradingAccountService;
  private final MarketPriceService marketPriceService;
  private final PositionPublisher positionPublisher;
  private final PositionCloseService positionCloseService;

  @Transactional
  public PositionResponse createPosition(UUID userId, CreatePositionRequest request) {
    UUID tradingAccountId = getTradingAccountId(userId);

    PositionResponse response = PositionResponse.from(createPositionForAccount(tradingAccountId, request));
    positionPublisher.publishPositionUpdate(userId, response);
    return response;
  }

  public Position createPositionForAccount(UUID tradingAccountId, CreatePositionRequest request) {
    Position position = new Position(
        tradingAccountId,
        request.symbol(),
        request.side(),
        request.quantity(),
        request.avgEntryPrice(),
        request.currentPrice(),
        request.marginUsed(),
        request.leverage(),
        request.takeProfitPrice(),
        request.stopLossPrice());

    return positionRepository.save(position);
  }

  public List<PositionResponse> getPositions(UUID userId, PositionStatus status) {
    UUID tradingAccountId = getTradingAccountId(userId);

    List<Position> positions = status == null
        ? positionRepository.findByTradingAccountIdOrderByOpenedAtDesc(tradingAccountId)
        : positionRepository.findByTradingAccountIdAndStatusOrderByOpenedAtDesc(tradingAccountId, status);

    return positions
        .stream()
        .map(this::toLiveResponse)
        .toList();
  }

  public PageResponse<PositionResponse> getPositions(UUID userId, PositionStatus status, int page, int size) {
    UUID tradingAccountId = getTradingAccountId(userId);
    Pageable pageable = pageRequest(page, size);

    if (status == null) {
      return PageResponse.from(positionRepository.findByTradingAccountIdOrderByOpenedAtDesc(tradingAccountId, pageable)
          .map(this::toLiveResponse));
    }

    if (status == PositionStatus.CLOSED) {
      return PageResponse.from(positionRepository
          .findByTradingAccountIdAndStatusOrderByClosedAtDesc(tradingAccountId, status, pageable)
          .map(this::toLiveResponse));
    }

    return PageResponse.from(positionRepository
        .findByTradingAccountIdAndStatusOrderByOpenedAtDesc(tradingAccountId, status, pageable)
        .map(this::toLiveResponse));
  }

  public PositionResponse getPosition(UUID userId, UUID positionId) {
    UUID tradingAccountId = getTradingAccountId(userId);

    Position position = positionRepository.findByIdAndTradingAccountId(positionId, tradingAccountId)
        .orElseThrow(() -> new NotFoundException("Position not found"));

    return toLiveResponse(position);
  }

  @Transactional
  public PositionResponse updateExitPrices(
      UUID userId,
      UUID positionId,
      UpdatePositionExitPricesRequest request) {
    TradingAccount account = tradingAccountService.getActiveAccount(userId);

    Position position = positionRepository.findByIdAndTradingAccountId(positionId, account.getId())
        .orElseThrow(() -> new NotFoundException("Position not found"));

    if (position.getStatus() != PositionStatus.OPEN) {
      throw new BadRequestException("Only open positions can update exit prices");
    }

    try {
      position.updateExitPrices(request.takeProfitPrice(), request.stopLossPrice());
    } catch (IllegalArgumentException exception) {
      throw new BadRequestException(exception.getMessage());
    }

    Optional<BigDecimal> cachedMarketPrice = getCachedMarketPrice(position.getSymbol());
    if (cachedMarketPrice
        .map(marketPrice -> position.shouldTakeProfit(marketPrice) || position.shouldStopLoss(marketPrice))
        .orElse(false)) {
      return positionCloseService.closeAtMarketPrice(userId, account, position, cachedMarketPrice.orElseThrow());
    }

    PositionResponse response = toLiveResponse(position);
    positionPublisher.publishPositionUpdate(userId, response);
    return response;
  }

  @Transactional
  public PositionResponse closePosition(UUID userId, UUID positionId) {
    TradingAccount account = tradingAccountService.getActiveAccount(userId);

    Position position = positionRepository.findByIdAndTradingAccountId(positionId, account.getId())
        .orElseThrow(() -> new NotFoundException("Position not found"));

    if (position.getStatus() != PositionStatus.OPEN) {
      throw new BadRequestException("Only open positions can be closed");
    }

    BigDecimal marketPrice = getMarketPrice(position.getSymbol());

    return positionCloseService.closeAtMarketPrice(userId, account, position, marketPrice);
  }

  private Optional<BigDecimal> getCachedMarketPrice(String symbol) {
    return marketPriceService.getCachedPrice(symbol)
        .map(MarketPrice::price)
        .filter(price -> price.signum() > 0);
  }

  private BigDecimal getMarketPrice(String symbol) {
    MarketPrice marketPrice = marketPriceService.refreshPrice(symbol);

    if (marketPrice == null || marketPrice.price() == null || marketPrice.price().signum() <= 0) {
      throw new BadRequestException("Market price is unavailable");
    }

    return marketPrice.price();
  }

  private PositionResponse toLiveResponse(Position position) {
    if (position.getStatus() != PositionStatus.OPEN) {
      return PositionResponse.from(position);
    }

    return marketPriceService.getCachedPrice(position.getSymbol())
        .map(marketPrice -> PositionResponse.from(
            position,
            marketPrice.price(),
            position.calculateUnrealizedPnl(marketPrice.price())))
        .orElseGet(() -> PositionResponse.from(position));
  }

  private UUID getTradingAccountId(UUID userId) {
    return tradingAccountService.getActiveAccount(userId).getId();
  }

  private Pageable pageRequest(int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);

    return PageRequest.of(safePage, safeSize);
  }
}
