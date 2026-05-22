import type { PositionResponse } from './positionApi'
import { subscribeToTopic } from './stompStream'

type PositionUpdateMessage = PositionResponse & {
  timestamp: string
}

type PositionUpdateHandler = (position: PositionResponse) => void

export function subscribeToPositionUpdates(
  userId: string,
  onPositionUpdate: PositionUpdateHandler,
) {
  return subscribeToTopic(
    `/topic/positions/${userId}`,
    `positions-${userId}`,
    (body) => {
      try {
        const message = JSON.parse(body) as PositionUpdateMessage
        onPositionUpdate(message)
      } catch {
        // Ignore malformed frames; REST fallback keeps the table recoverable.
      }
    },
  )
}
