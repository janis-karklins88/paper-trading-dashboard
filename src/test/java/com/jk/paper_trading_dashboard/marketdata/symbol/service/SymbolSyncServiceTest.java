package com.jk.paper_trading_dashboard.marketdata.symbol.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jk.paper_trading_dashboard.alpaca.dto.AlpacaAssetDto;
import com.jk.paper_trading_dashboard.alpaca.service.AlpacaAssetClient;
import com.jk.paper_trading_dashboard.marketdata.symbol.domain.AssetType;
import com.jk.paper_trading_dashboard.marketdata.symbol.domain.Symbol;
import com.jk.paper_trading_dashboard.marketdata.symbol.repository.SymbolRepository;

@ExtendWith(MockitoExtension.class)
class SymbolSyncServiceTest {

  @Mock
  private AlpacaAssetClient alpacaAssetClient;

  @Mock
  private SymbolRepository symbolRepository;

  private SymbolSyncService symbolSyncService;

  @BeforeEach
  void setUp() {
    symbolSyncService = new SymbolSyncService(alpacaAssetClient, symbolRepository);
  }

  @Test
  @SuppressWarnings("unchecked")
  void syncSymbolsUpdatesExistingSymbolsAndSavesOnlyNewOnes() {
    Symbol existingSymbol = new Symbol("AAPL", "Old Name", AssetType.STOCK, "NASDAQ", true, true);
    when(alpacaAssetClient.getAssets()).thenReturn(List.of(
        new AlpacaAssetDto("AAPL", "Apple Inc.", "us_equity", null, "NASDAQ", "active", true),
        new AlpacaAssetDto("BTC/USD", "Bitcoin", "crypto", null, "CRYPTO", "active", true)));
    when(symbolRepository.findAllBySymbolIn(anyCollection())).thenReturn(List.of(existingSymbol));

    int syncedCount = symbolSyncService.syncSymbols();

    assertThat(syncedCount).isEqualTo(2);
    assertThat(existingSymbol.getDisplayName()).isEqualTo("Apple Inc.");

    ArgumentCaptor<List<Symbol>> savedSymbolsCaptor = ArgumentCaptor.forClass(List.class);
    verify(symbolRepository).saveAll(savedSymbolsCaptor.capture());
    assertThat(savedSymbolsCaptor.getValue())
        .extracting(Symbol::getSymbol)
        .containsExactly("BTC/USD");
  }

  @Test
  @SuppressWarnings("unchecked")
  void syncSymbolsSkipsAssetsWithMissingAssetClass() {
    when(alpacaAssetClient.getAssets()).thenReturn(List.of(
        new AlpacaAssetDto("AAPL", "Apple Inc.", null, null, "NASDAQ", "active", true),
        new AlpacaAssetDto("BTC/USD", "Bitcoin", "crypto", null, "CRYPTO", "active", true)));
    when(symbolRepository.findAllBySymbolIn(anyCollection())).thenReturn(List.of());

    int syncedCount = symbolSyncService.syncSymbols();

    assertThat(syncedCount).isEqualTo(1);

    ArgumentCaptor<List<Symbol>> savedSymbolsCaptor = ArgumentCaptor.forClass(List.class);
    verify(symbolRepository).saveAll(savedSymbolsCaptor.capture());
    assertThat(savedSymbolsCaptor.getValue())
        .extracting(Symbol::getSymbol)
        .containsExactly("BTC/USD");
  }
}
