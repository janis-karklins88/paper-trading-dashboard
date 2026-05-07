package com.jk.paper_trading_dashboard.shared.exception;

import java.time.Instant;

public record ApiErrorResponse(
    String message,
    String path,
    Instant timestamp) {
}
