package com.jk.paper_trading_dashboard.position.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;

public record UpdatePositionExitPricesRequest(
    @DecimalMin("0.00000001") BigDecimal takeProfitPrice,
    @DecimalMin("0.00000001") BigDecimal stopLossPrice) {

}
