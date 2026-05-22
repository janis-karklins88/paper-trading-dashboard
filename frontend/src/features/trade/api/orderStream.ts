import type { OrderResponse } from './orderApi'
import { subscribeToTopic } from './stompStream'

type OrderUpdateMessage = OrderResponse & {
  timestamp: string
}

type OrderUpdateHandler = (order: OrderResponse) => void

export function subscribeToOrderUpdates(
  userId: string,
  onOrderUpdate: OrderUpdateHandler,
) {
  return subscribeToTopic(
    `/topic/orders/${userId}`,
    `orders-${userId}`,
    (body) => {
      try {
        const message = JSON.parse(body) as OrderUpdateMessage
        onOrderUpdate(message)
      } catch {
        // Ignore malformed frames; REST recovery polling keeps orders usable.
      }
    },
  )
}
