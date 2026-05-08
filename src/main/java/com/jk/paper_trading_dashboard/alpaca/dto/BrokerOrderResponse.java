package com.jk.paper_trading_dashboard.alpaca.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BrokerOrderResponse(
    String brokerOrderId,
    String brokerStatus,
    BigDecimal filledQuantity,
    BigDecimal averageFillPrice,
    String rejectionReason,
    Instant submittedAt) {

}
