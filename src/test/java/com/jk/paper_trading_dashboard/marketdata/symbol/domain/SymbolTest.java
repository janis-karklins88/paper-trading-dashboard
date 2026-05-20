package com.jk.paper_trading_dashboard.marketdata.symbol.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SymbolTest {

  @Test
  void constructorNormalizesAndStoresSymbolDetails() {
    Symbol symbol = new Symbol(
        "aapl",
        "Apple Inc.",
        AssetType.STOCK,
        "NASDAQ",
        true,
        true);

    assertThat(symbol.getSymbol()).isEqualTo("AAPL");
    assertThat(symbol.getDisplayName()).isEqualTo("Apple Inc.");
    assertThat(symbol.getAssetType()).isEqualTo(AssetType.STOCK);
    assertThat(symbol.getExchange()).isEqualTo("NASDAQ");
    assertThat(symbol.isActive()).isTrue();
    assertThat(symbol.isTradable()).isTrue();
  }

  @Test
  void updateFromRefreshesMutableFieldsWithoutChangingSymbol() {
    Symbol symbol = new Symbol("AAPL", "Old Name", AssetType.STOCK, "NASDAQ", true, true);
    Symbol source = new Symbol("AAPL", "Apple Inc.", AssetType.STOCK, "NYSE", false, false);

    symbol.updateFrom(source);

    assertThat(symbol.getSymbol()).isEqualTo("AAPL");
    assertThat(symbol.getDisplayName()).isEqualTo("Apple Inc.");
    assertThat(symbol.getExchange()).isEqualTo("NYSE");
    assertThat(symbol.isActive()).isFalse();
    assertThat(symbol.isTradable()).isFalse();
  }
}
