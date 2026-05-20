package com.jk.paper_trading_dashboard.marketdata.symbol.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.jk.paper_trading_dashboard.marketdata.symbol.domain.AssetType;
import com.jk.paper_trading_dashboard.marketdata.symbol.domain.Symbol;
import com.jk.paper_trading_dashboard.marketdata.symbol.dto.SymbolResponse;
import com.jk.paper_trading_dashboard.marketdata.symbol.repository.SymbolRepository;
import com.jk.paper_trading_dashboard.shared.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class SymbolServiceTest {

  @Mock
  private SymbolRepository symbolRepository;

  private SymbolService symbolService;

  @BeforeEach
  void setUp() {
    symbolService = new SymbolService(symbolRepository);
  }

  @Test
  void searchSymbolsNormalizesQueryAndCapsPageSize() {
    Symbol apple = new Symbol("AAPL", "Apple Inc.", AssetType.STOCK, "NASDAQ", true, true);
    when(symbolRepository.search(eq("app"), eq(AssetType.STOCK), eq(true), eq(true), org.mockito.ArgumentMatchers.any()))
        .thenReturn(new PageImpl<>(List.of(apple)));

    PageResponse<SymbolResponse> response = symbolService.searchSymbols(" app ", AssetType.STOCK, true, true, -1, 500);

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().getFirst().symbol()).isEqualTo("AAPL");

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(symbolRepository).search(eq("app"), eq(AssetType.STOCK), eq(true), eq(true), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
  }
}
