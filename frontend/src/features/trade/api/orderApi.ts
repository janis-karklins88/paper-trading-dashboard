import { getStoredAuthToken } from '../../../auth/authApi'

export type PlaceOrderPayload = {
  symbol: string
  side: 'BUY' | 'SELL'
  type: 'MARKET' | 'LIMIT'
  marginAmount: string
  leverage: string
  limitPrice: string | null
  takeProfitPrice: string | null
  stopLossPrice: string | null
}

export type OrderResponse = {
  id: string
  symbol: string
  side: 'BUY' | 'SELL'
  type: 'MARKET' | 'LIMIT'
  status: string
  quantity: string | number | null
  marginAmount: string | number
  leverage: string | number
  notionalValue: string | number
  feeAmount: string | number
  executionPrice: string | number | null
  limitPrice: string | number | null
  takeProfitPrice: string | number | null
  stopLossPrice: string | number | null
  rejectReason: string | null
  createdAt: string
  openedAt: string | null
  filledAt: string | null
  updatedAt: string
}

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

export async function placeOrder(payload: PlaceOrderPayload) {
  const token = getStoredAuthToken()

  const response = await fetch('/api/orders', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    throw new Error(await getErrorMessage(response))
  }

  return (await response.json()) as OrderResponse
}

export async function getOrders(page = 0, size = 25) {
  const token = getStoredAuthToken()

  const response = await fetch(`/api/orders?page=${page}&size=${size}`, {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error(await getErrorMessage(response))
  }

  return (await response.json()) as PageResponse<OrderResponse>
}

export async function cancelOrder(orderId: string) {
  const token = getStoredAuthToken()

  const response = await fetch(`/api/orders/${orderId}/cancel`, {
    method: 'POST',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error(await getErrorMessage(response))
  }

  return (await response.json()) as OrderResponse
}

async function getErrorMessage(response: Response) {
  try {
    const data = (await response.json()) as ApiErrorResponse
    return data.message || 'Failed to place order'
  } catch {
    return 'Failed to place order'
  }
}
