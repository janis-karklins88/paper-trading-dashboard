package com.jk.paper_trading_dashboard.alpaca.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.jk.paper_trading_dashboard.alpaca.config.AlpacaProperties;
import com.jk.paper_trading_dashboard.alpaca.dto.AlpacaAssetDto;
import com.jk.paper_trading_dashboard.marketdata.domain.Symbols;

@Service
public class AlpacaAssetClient {

  private static final List<String> SUPPORTED_ASSET_CLASSES = List.of("us_equity", "crypto");
  private static final List<String> SYNC_STATUSES = List.of("active", "inactive");

  private final RestClient restClient;

  public AlpacaAssetClient(RestClient.Builder restClientBuilder, AlpacaProperties alpacaProperties) {
    this.restClient = restClientBuilder
        .baseUrl(alpacaProperties.tradingBaseUrl())
        .defaultHeader("APCA-API-KEY-ID", alpacaProperties.apiKey())
        .defaultHeader("APCA-API-SECRET-KEY", alpacaProperties.secretKey())
        .build();
  }

  public List<AlpacaAssetDto> getAssets() {
    Map<String, AlpacaAssetDto> assetsBySymbol = new LinkedHashMap<>();

    for (String assetClass : SUPPORTED_ASSET_CLASSES) {
      for (String status : SYNC_STATUSES) {
        for (AlpacaAssetDto asset : getAssets(assetClass, status)) {
          if (asset.symbol() == null || asset.symbol().isBlank()) {
            continue;
          }

          assetsBySymbol.put(Symbols.normalize(asset.symbol()), asset);
        }
      }
    }

    return new ArrayList<>(assetsBySymbol.values());
  }

  private List<AlpacaAssetDto> getAssets(String assetClass, String status) {
    AlpacaAssetDto[] response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/v2/assets")
            .queryParam("asset_class", assetClass)
            .queryParam("status", status)
            .build())
        .retrieve()
        .body(AlpacaAssetDto[].class);

    return response == null ? List.of() : List.of(response);
  }
}
