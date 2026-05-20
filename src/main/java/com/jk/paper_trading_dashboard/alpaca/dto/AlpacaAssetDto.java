package com.jk.paper_trading_dashboard.alpaca.dto;

public record AlpacaAssetDto(
    String symbol,
    String name,
    String asset_class,
    String exchange,
    String status,
    Boolean tradable) {

}
