package com.jk.paper_trading_dashboard.marketdata.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.marketdata.domain.DefaultCryptoSymbol;
import com.jk.paper_trading_dashboard.marketdata.domain.DefaultMarketSymbol;

@Service
public class DefaultMarketSymbolService {

  private static final List<DefaultCryptoSymbol> TOP_CRYPTO_SYMBOLS = List.of(
      new DefaultCryptoSymbol(1, "BTC", "Bitcoin", "BTC/USD"),
      new DefaultCryptoSymbol(2, "ETH", "Ethereum", "ETH/USD"),
      new DefaultCryptoSymbol(3, "USDT", "Tether", "USDT/USD"),
      new DefaultCryptoSymbol(4, "BNB", "BNB", "BNB/USD"),
      new DefaultCryptoSymbol(5, "XRP", "XRP", "XRP/USD"),
      new DefaultCryptoSymbol(6, "USDC", "USDC", "USDC/USD"),
      new DefaultCryptoSymbol(7, "SOL", "Solana", "SOL/USD"),
      new DefaultCryptoSymbol(8, "TRX", "TRON", "TRX/USD"),
      new DefaultCryptoSymbol(9, "DOGE", "Dogecoin", "DOGE/USD"),
      new DefaultCryptoSymbol(10, "ADA", "Cardano", "ADA/USD"),
      new DefaultCryptoSymbol(11, "ZEC", "Zcash", "ZEC/USD"),
      new DefaultCryptoSymbol(12, "BCH", "Bitcoin Cash", "BCH/USD"),
      new DefaultCryptoSymbol(13, "LINK", "Chainlink", "LINK/USD"),
      new DefaultCryptoSymbol(14, "XMR", "Monero", "XMR/USD"),
      new DefaultCryptoSymbol(15, "TON", "Toncoin", "TON/USD"),
      new DefaultCryptoSymbol(16, "XLM", "Stellar", "XLM/USD"),
      new DefaultCryptoSymbol(17, "SUI", "Sui", "SUI/USD"),
      new DefaultCryptoSymbol(18, "LTC", "Litecoin", "LTC/USD"),
      new DefaultCryptoSymbol(19, "AVAX", "Avalanche", "AVAX/USD"),
      new DefaultCryptoSymbol(20, "HBAR", "Hedera", "HBAR/USD"));

  private static final List<DefaultMarketSymbol> POPULAR_STOCK_SYMBOLS = List.of(
      new DefaultMarketSymbol(1, "AAPL", "Apple, Inc", "AAPL"),
      new DefaultMarketSymbol(2, "MSFT", "Microsoft Corporation", "MSFT"),
      new DefaultMarketSymbol(3, "NVDA", "NVIDIA Corporation", "NVDA"),
      new DefaultMarketSymbol(4, "TSLA", "Tesla, Inc", "TSLA"),
      new DefaultMarketSymbol(5, "AMZN", "Amazon.com, Inc", "AMZN"),
      new DefaultMarketSymbol(6, "META", "Meta Platforms, Inc", "META"),
      new DefaultMarketSymbol(7, "GOOGL", "Alphabet Inc", "GOOGL"),
      new DefaultMarketSymbol(8, "AMD", "Advanced Micro Devices, Inc", "AMD"),
      new DefaultMarketSymbol(9, "NFLX", "Netflix, Inc", "NFLX"),
      new DefaultMarketSymbol(10, "COIN", "Coinbase Global, Inc", "COIN"));

  public List<DefaultCryptoSymbol> getTopCryptoSymbols() {
    return TOP_CRYPTO_SYMBOLS;
  }

  public List<DefaultMarketSymbol> getPopularStockSymbols() {
    return POPULAR_STOCK_SYMBOLS;
  }
}
