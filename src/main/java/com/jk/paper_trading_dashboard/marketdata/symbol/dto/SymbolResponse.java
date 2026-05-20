package com.jk.paper_trading_dashboard.marketdata.symbol.dto;

import java.util.UUID;

import com.jk.paper_trading_dashboard.marketdata.symbol.domain.AssetType;
import com.jk.paper_trading_dashboard.marketdata.symbol.domain.Symbol;

public record SymbolResponse(
    UUID id,
    String symbol,
    String displayName,
    AssetType assetType,
    String exchange,
    boolean active,
    boolean tradable) {

  public static SymbolResponse from(Symbol symbol) {
    return new SymbolResponse(
        symbol.getId(),
        symbol.getSymbol(),
        symbol.getDisplayName(),
        symbol.getAssetType(),
        symbol.getExchange(),
        symbol.isActive(),
        symbol.isTradable());
  }
}
