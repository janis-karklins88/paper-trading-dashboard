import { getStoredAuthToken } from '../../../auth/authApi'

export type PositionResponse = {
  id: string
  symbol: string
  side: 'LONG' | 'SHORT'
  status: 'OPEN' | 'CLOSED'
  quantity: string | number
  avgEntryPrice: string | number
  currentPrice: string | number
  unrealizedPnl: string | number
  realizedPnl: string | number
  marginUsed: string | number
  leverage: string | number
  openedAt: string
  closedAt: string | null
  updatedAt: string
}

export type PositionStatus = 'OPEN' | 'CLOSED'

type ApiErrorResponse = {
  message?: string
}

export async function getPositions(status: PositionStatus) {
  const token = getStoredAuthToken()

  const response = await fetch(`/api/positions?status=${status}`, {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error(await getErrorMessage(response))
  }

  return (await response.json()) as PositionResponse[]
}

export async function getOpenPositions() {
  return getPositions('OPEN')
}

export async function getClosedPositions() {
  return getPositions('CLOSED')
}

export async function closePosition(positionId: string) {
  const token = getStoredAuthToken()

  const response = await fetch(`/api/positions/${positionId}/close`, {
    method: 'POST',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error(await getErrorMessage(response))
  }

  return (await response.json()) as PositionResponse
}

async function getErrorMessage(response: Response) {
  try {
    const data = (await response.json()) as ApiErrorResponse
    return data.message || 'Failed to update position'
  } catch {
    return 'Failed to update position'
  }
}
