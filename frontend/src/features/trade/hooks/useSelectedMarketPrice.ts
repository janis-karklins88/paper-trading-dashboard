import { useEffect, useState } from 'react'
import { getLatestPrice, type MarketPrice } from '../api/marketDataApi'
import { subscribeToPriceUpdates } from '../api/marketDataStream'

const FALLBACK_PRICE_REFRESH_MS = 15_000

export function useSelectedMarketPrice(symbol: string) {
  const [price, setPrice] = useState<MarketPrice | null>(null)

  useEffect(() => {
    if (!symbol) {
      setPrice(null)
      return
    }

    let isMounted = true

    const loadLatestPrice = () => {
      getLatestPrice(symbol)
        .then((nextPrice) => {
          if (isMounted) {
            setPrice(nextPrice)
          }
        })
        .catch(() => {
          if (isMounted) {
            setPrice(null)
          }
        })
    }

    loadLatestPrice()

    const unsubscribe = subscribeToPriceUpdates(symbol, (nextPrice) => {
      if (isMounted) {
        setPrice(nextPrice)
      }
    })

    const intervalId = window.setInterval(
      loadLatestPrice,
      FALLBACK_PRICE_REFRESH_MS,
    )

    return () => {
      isMounted = false
      unsubscribe()
      window.clearInterval(intervalId)
    }
  }, [symbol])

  return price
}
