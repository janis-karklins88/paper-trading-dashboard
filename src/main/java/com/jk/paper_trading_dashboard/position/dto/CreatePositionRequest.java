package com.jk.paper_trading_dashboard.position.dto;

import java.math.BigDecimal;

import com.jk.paper_trading_dashboard.position.domain.PositionSide;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePositionRequest(
    @NotBlank String symbol,
    @NotNull PositionSide side,
    @NotNull @DecimalMin("0.00000001") BigDecimal quantity,
    @NotNull @DecimalMin("0.00000001") BigDecimal avgEntryPrice,
    @NotNull @DecimalMin("0.00000001") BigDecimal currentPrice,
    @NotNull @DecimalMin("0.00000001") BigDecimal marginUsed,
    @NotNull @DecimalMin("1.00") BigDecimal leverage) {

}
