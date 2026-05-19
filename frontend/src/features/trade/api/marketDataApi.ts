import { getStoredAuthToken } from '../../../auth/authApi'

export type CandleTimeframe = '1m' | '5m' | '15m' | '1h' | '1d'

export type DefaultMarketSymbol = {
  rank: number
  symbol: string
  name: string
  quoteSymbol: string
}

export type MarketCandle = {
  timestamp: string
  open: number | string
  high: number | string
  low: number | string
  close: number | string
  volume: number | string
}

export type MarketPrice = {
  symbol: string
  price: number | string
  updatedAt: string
}

export async function getDefaultCryptoSymbols() {
  return fetchJson<DefaultMarketSymbol[]>('/api/market-data/defaults/crypto')
}

export async function getDefaultStockSymbols() {
  return fetchJson<DefaultMarketSymbol[]>('/api/market-data/defaults/stocks')
}

export async function getCandles(symbol: string, timeframe: CandleTimeframe) {
  const params = new URLSearchParams({ symbol, timeframe })
  return fetchJson<MarketCandle[]>(`/api/market-data/candles?${params}`)
}

export async function getLatestPrice(symbol: string) {
  const params = new URLSearchParams({ symbol })
  return fetchJson<MarketPrice>(`/api/market-data/latest-price?${params}`)
}

async function fetchJson<T>(url: string) {
  const token = getStoredAuthToken()
  const response = await fetch(url, {
    headers: token
      ? {
          Authorization: `Bearer ${token}`,
        }
      : undefined,
  })

  if (!response.ok) {
    throw new Error('Failed to load market data')
  }

  return (await response.json()) as T
}
