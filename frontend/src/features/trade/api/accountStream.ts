import type { TradingAccountResponse } from './accountApi'
import { subscribeToTopic } from './stompStream'

type TradingAccountUpdateMessage = TradingAccountResponse & {
  timestamp: string
}

type TradingAccountUpdateHandler = (account: TradingAccountResponse) => void

export function subscribeToTradingAccountUpdates(
  userId: string,
  onAccountUpdate: TradingAccountUpdateHandler,
) {
  return subscribeToTopic(
    `/topic/portfolio/${userId}`,
    `portfolio-${userId}`,
    (body) => {
      try {
        const message = JSON.parse(body) as TradingAccountUpdateMessage
        onAccountUpdate(message)
      } catch {
        // Ignore malformed frames; REST polling keeps account values recoverable.
      }
    },
  )
}
