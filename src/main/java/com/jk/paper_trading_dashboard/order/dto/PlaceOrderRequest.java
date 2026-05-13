package com.jk.paper_trading_dashboard.order.dto;

import java.math.BigDecimal;

import com.jk.paper_trading_dashboard.order.domain.OrderSide;
import com.jk.paper_trading_dashboard.order.domain.OrderType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequest(
    @NotBlank String symbol,
    @NotNull OrderSide side,
    @NotNull OrderType type,
    @NotNull @DecimalMin("0.0001") BigDecimal marginAmount,
    @NotNull @DecimalMin("1.00") BigDecimal leverage,
    @DecimalMin("0.0001") BigDecimal limitPrice,
    @DecimalMin("0.0001") BigDecimal takeProfitPrice,
    @DecimalMin("0.0001") BigDecimal stopLossPrice) {

}
