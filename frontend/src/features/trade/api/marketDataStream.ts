import type { MarketPrice } from './marketDataApi'
import { subscribeToTopic } from './stompStream'

type PriceUpdateMessage = {
  symbol: string
  price: number | string
  timestamp: string
}

type PriceUpdateHandler = (price: MarketPrice) => void

export function subscribeToPriceUpdates(
  symbol: string,
  onPriceUpdate: PriceUpdateHandler,
) {
  const topic = `/topic/prices/${normalizeTopicSymbol(symbol)}`
  return subscribeToTopic(
    topic,
    `price-${normalizeTopicSymbol(symbol).replaceAll('/', '-')}`,
    (body) => {
      try {
        const message = JSON.parse(body) as PriceUpdateMessage
        onPriceUpdate({
          symbol: message.symbol,
          price: message.price,
          updatedAt: message.timestamp,
        })
      } catch {
        // Ignore malformed frames; REST fallback will keep the displayed price usable.
      }
    },
  )

  function normalizeTopicSymbol(value: string) {
    return value.trim().toUpperCase()
  }
}
