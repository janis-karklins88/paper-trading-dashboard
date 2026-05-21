package com.jk.paper_trading_dashboard.alpaca.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class AlpacaAssetDtoTest {

  @Test
  void mapsAssetClassFromAlpacaClassField() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    AlpacaAssetDto dto = objectMapper.readValue("""
        {
          "class": "crypto",
          "exchange": "CRYPTO",
          "symbol": "ETH/USD",
          "name": "Ethereum / US Dollar",
          "status": "active",
          "tradable": true
        }
        """, AlpacaAssetDto.class);

    assertThat(dto.resolvedAssetClass()).isEqualTo("crypto");
    assertThat(dto.symbol()).isEqualTo("ETH/USD");
  }
}
