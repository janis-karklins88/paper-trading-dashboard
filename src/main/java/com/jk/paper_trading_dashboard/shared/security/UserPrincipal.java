package com.jk.paper_trading_dashboard.shared.security;

import java.util.UUID;

public record UserPrincipal(
    UUID userId,
    String email) {
}
