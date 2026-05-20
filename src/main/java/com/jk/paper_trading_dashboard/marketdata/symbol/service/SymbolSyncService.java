package com.jk.paper_trading_dashboard.marketdata.symbol.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jk.paper_trading_dashboard.alpaca.dto.AlpacaAssetDto;
import com.jk.paper_trading_dashboard.alpaca.service.AlpacaAssetClient;
import com.jk.paper_trading_dashboard.marketdata.symbol.domain.AssetType;
import com.jk.paper_trading_dashboard.marketdata.symbol.domain.Symbol;
import com.jk.paper_trading_dashboard.marketdata.symbol.repository.SymbolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SymbolSyncService {

  private final AlpacaAssetClient alpacaAssetClient;
  private final SymbolRepository symbolRepository;

  @Transactional
  public int syncSymbols() {
    Map<String, Symbol> incomingSymbols = alpacaAssetClient.getAssets()
        .stream()
        .map(this::toSymbol)
        .collect(Collectors.toMap(
            Symbol::getSymbol,
            Function.identity(),
            (first, second) -> second,
            LinkedHashMap::new));

    if (incomingSymbols.isEmpty()) {
      return 0;
    }

    Map<String, Symbol> existingSymbols = symbolRepository.findAllBySymbolIn(incomingSymbols.keySet())
        .stream()
        .collect(Collectors.toMap(Symbol::getSymbol, Function.identity()));

    List<Symbol> newSymbols = incomingSymbols.values()
        .stream()
        .filter(symbol -> {
          Symbol existingSymbol = existingSymbols.get(symbol.getSymbol());

          if (existingSymbol == null) {
            return true;
          }

          existingSymbol.updateFrom(symbol);
          return false;
        })
        .toList();

    symbolRepository.saveAll(newSymbols);
    return incomingSymbols.size();
  }

  private Symbol toSymbol(AlpacaAssetDto dto) {
    return new Symbol(
        dto.symbol(),
        dto.name(),
        mapAssetType(dto.asset_class()),
        dto.exchange(),
        "active".equalsIgnoreCase(normalizeNullable(dto.status())),
        Boolean.TRUE.equals(dto.tradable()));
  }

  private AssetType mapAssetType(String assetClass) {
    String normalizedAssetClass = normalizeNullable(assetClass);

    if (normalizedAssetClass == null) {
      throw new IllegalArgumentException("asset_class is required");
    }

    return switch (normalizedAssetClass.toLowerCase(Locale.ROOT)) {
      case "us_equity", "equity", "stock" -> AssetType.STOCK;
      case "crypto" -> AssetType.CRYPTO;
      default -> throw new IllegalArgumentException("Unsupported Alpaca asset class: " + assetClass);
    };
  }

  private String normalizeNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return value.trim();
  }
}
