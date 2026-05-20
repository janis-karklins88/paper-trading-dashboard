package com.jk.paper_trading_dashboard.marketdata.symbol.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.marketdata.symbol.domain.AssetType;
import com.jk.paper_trading_dashboard.marketdata.symbol.dto.SymbolResponse;
import com.jk.paper_trading_dashboard.marketdata.symbol.repository.SymbolRepository;
import com.jk.paper_trading_dashboard.shared.dto.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SymbolService {

  private static final int MAX_PAGE_SIZE = 100;

  private final SymbolRepository symbolRepository;

  public PageResponse<SymbolResponse> searchSymbols(
      String query,
      AssetType assetType,
      Boolean active,
      Boolean tradable,
      int page,
      int size) {
    PageRequest pageRequest = PageRequest.of(
        Math.max(page, 0),
        Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
        Sort.by(Sort.Order.asc("symbol")));

    return PageResponse.from(symbolRepository.search(
        normalizeQuery(query),
        assetType,
        active,
        tradable,
        pageRequest).map(SymbolResponse::from));
  }

  private String normalizeQuery(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }

    return query.trim();
  }
}
