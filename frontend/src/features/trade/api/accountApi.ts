import { getStoredAuthToken } from '../../../auth/authApi'

export type TradingAccountResponse = {
  cashBalance: string | number
  reservedMargin: string | number
  equity: string | number
  unrealizedPnl: string | number
  realizedPnl: string | number
  maxLeverage: string | number
}

type ApiErrorResponse = {
  message?: string
}

export async function getTradingAccount() {
  const token = getStoredAuthToken()

  const response = await fetch('/api/trading-account', {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error(await getErrorMessage(response))
  }

  return (await response.json()) as TradingAccountResponse
}

async function getErrorMessage(response: Response) {
  try {
    const data = (await response.json()) as ApiErrorResponse
    return data.message || 'Failed to load trading account'
  } catch {
    return 'Failed to load trading account'
  }
}
