package com.jk.paper_trading_dashboard.marketdata.symbol.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jk.paper_trading_dashboard.marketdata.symbol.domain.AssetType;
import com.jk.paper_trading_dashboard.marketdata.symbol.dto.SymbolResponse;
import com.jk.paper_trading_dashboard.marketdata.symbol.service.SymbolService;
import com.jk.paper_trading_dashboard.shared.dto.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/symbols")
@RequiredArgsConstructor
public class SymbolController {

  private final SymbolService symbolService;

  @GetMapping
  public ResponseEntity<PageResponse<SymbolResponse>> searchSymbols(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) AssetType assetType,
      @RequestParam(required = false, defaultValue = "true") Boolean active,
      @RequestParam(required = false, defaultValue = "true") Boolean tradable,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return ResponseEntity.ok(symbolService.searchSymbols(query, assetType, active, tradable, page, size));
  }
}
