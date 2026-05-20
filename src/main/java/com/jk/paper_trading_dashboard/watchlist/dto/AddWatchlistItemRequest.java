package com.jk.paper_trading_dashboard.watchlist.dto;

import jakarta.validation.constraints.NotBlank;

public record AddWatchlistItemRequest(
    @NotBlank String symbol) {

}
