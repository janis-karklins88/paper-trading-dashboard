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

export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

type ApiErrorResponse = {
  message?: string
}

export async function getPositions(status: PositionStatus, page = 0, size = 25) {
  const token = getStoredAuthToken()

  const response = await fetch(`/api/positions?status=${status}&page=${page}&size=${size}`, {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error(await getErrorMessage(response))
  }

  return (await response.json()) as PageResponse<PositionResponse>
}

export async function getOpenPositions(page = 0, size = 25) {
  return getPositions('OPEN', page, size)
}

export async function getClosedPositions(page = 0, size = 25) {
  return getPositions('CLOSED', page, size)
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
