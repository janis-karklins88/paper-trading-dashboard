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

export async function getLatestPrices(symbols: string[]) {
  return fetchJson<MarketPrice[]>('/api/market-data/latest-prices', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ symbols }),
  })
}

export async function trackActiveSymbol(symbol: string) {
  await fetchJson<void>('/api/market-data/active-symbol', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ symbol }),
  })
}

async function fetchJson<T>(url: string, init?: RequestInit) {
  const token = getStoredAuthToken()
  const response = await fetch(url, {
    ...init,
    headers: {
      ...init?.headers,
      ...(token
      ? {
          Authorization: `Bearer ${token}`,
        }
      : {}),
    },
  })

  if (!response.ok) {
    throw new Error('Failed to load market data')
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
