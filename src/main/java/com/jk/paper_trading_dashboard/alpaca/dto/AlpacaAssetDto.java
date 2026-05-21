package com.jk.paper_trading_dashboard.alpaca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AlpacaAssetDto(
    String symbol,
    String name,
    @JsonProperty("class") String assetClass,
    @JsonProperty("asset_class") String asset_class,
    String exchange,
    String status,
    Boolean tradable) {

  public String resolvedAssetClass() {
    return assetClass != null ? assetClass : asset_class;
  }
}
