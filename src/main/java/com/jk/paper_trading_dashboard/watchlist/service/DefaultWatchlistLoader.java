package com.jk.paper_trading_dashboard.watchlist.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jk.paper_trading_dashboard.watchlist.config.DefaultWatchlistConfig;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultWatchlistLoader {
  private final ObjectMapper objectMapper;

  public List<DefaultWatchlistConfig> load() {
    try (InputStream is = getClass().getResourceAsStream("/watchlists/default-watchlists.json")) {
      if (is == null) {
        throw new IllegalStateException("Default watchlists config file is missing");
      }

      return objectMapper.readValue(is, new TypeReference<>() {
      });
    } catch (IOException e) {
      throw new RuntimeException("Failed to load default watchlists", e);
    }
  }
}
