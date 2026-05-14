package com.jk.paper_trading_dashboard.alpaca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alpaca")
public record AlpacaProperties(
    String baseUrl,
    String apiKey,
    String secretKey) {

}
