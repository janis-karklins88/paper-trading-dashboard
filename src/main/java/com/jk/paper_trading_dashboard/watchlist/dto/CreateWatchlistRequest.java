package com.jk.paper_trading_dashboard.watchlist.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWatchlistRequest(
    @NotBlank String name) {

}
