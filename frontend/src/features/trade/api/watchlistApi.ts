import { getStoredAuthToken } from '../../../auth/authApi'

export type WatchlistResponse = {
  id: string
  userId: string
  name: string
  defaultWatchlist: boolean
  createdAt: string
}

export type WatchlistItemResponse = {
  id: string
  watchlistId: string
  symbol: string
  sortOrder: number
  createdAt: string
}

export type WatchlistDetailResponse = WatchlistResponse & {
  items: WatchlistItemResponse[]
}

type ApiErrorResponse = {
  message?: string
}

export async function getWatchlists() {
  return fetchJson<WatchlistResponse[]>('/api/watchlists')
}

export async function createWatchlist(name: string) {
  return fetchJson<WatchlistResponse>('/api/watchlists', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ name }),
  })
}

export async function deleteWatchlist(watchlistId: string) {
  await fetchJson<void>(`/api/watchlists/${watchlistId}`, {
    method: 'DELETE',
  })
}

export async function getWatchlist(watchlistId: string) {
  return fetchJson<WatchlistDetailResponse>(`/api/watchlists/${watchlistId}`)
}

export async function addWatchlistItem(watchlistId: string, symbol: string) {
  return fetchJson<WatchlistItemResponse>(
    `/api/watchlists/${watchlistId}/items`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ symbol }),
    },
  )
}

export async function removeWatchlistItem(
  watchlistId: string,
  itemId: string,
) {
  await fetchJson<void>(`/api/watchlists/${watchlistId}/items/${itemId}`, {
    method: 'DELETE',
  })
}

async function fetchJson<T>(url: string, init?: RequestInit) {
  const token = getStoredAuthToken()
  const response = await fetch(url, {
    ...init,
    headers: {
      ...init?.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error(await getErrorMessage(response))
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

async function getErrorMessage(response: Response) {
  try {
    const data = (await response.json()) as ApiErrorResponse
    return data.message || 'Failed to update watchlist'
  } catch {
    return 'Failed to update watchlist'
  }
}
