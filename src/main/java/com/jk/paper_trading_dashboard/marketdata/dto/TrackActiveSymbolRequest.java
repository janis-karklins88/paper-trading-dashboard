package com.jk.paper_trading_dashboard.marketdata.dto;

import jakarta.validation.constraints.NotBlank;

public record TrackActiveSymbolRequest(
    @NotBlank String symbol) {

}
