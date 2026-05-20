package com.jk.paper_trading_dashboard.marketdata.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record LatestPricesRequest(
    @NotEmpty List<String> symbols) {

}
