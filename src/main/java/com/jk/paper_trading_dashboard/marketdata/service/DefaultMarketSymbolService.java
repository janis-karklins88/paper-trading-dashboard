package com.jk.paper_trading_dashboard.marketdata.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.jk.paper_trading_dashboard.marketdata.domain.DefaultCryptoSymbol;

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

  public List<DefaultCryptoSymbol> getTopCryptoSymbols() {
    return TOP_CRYPTO_SYMBOLS;
  }
}
