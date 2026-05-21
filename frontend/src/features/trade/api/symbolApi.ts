import { getStoredAuthToken } from '../../../auth/authApi'

export type SymbolSearchResult = {
  id: string
  symbol: string
  displayName: string
  assetType: 'STOCK' | 'CRYPTO'
  exchange: string | null
  active: boolean
  tradable: boolean
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

export async function searchSymbols(query: string) {
  const params = new URLSearchParams({
    query,
    active: 'true',
    tradable: 'true',
    size: '12',
  })

  return fetchJson<PageResponse<SymbolSearchResult>>(`/api/symbols?${params}`)
}

async function fetchJson<T>(url: string) {
  const token = getStoredAuthToken()
  const response = await fetch(url, {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error(await getErrorMessage(response))
  }

  return (await response.json()) as T
}

async function getErrorMessage(response: Response) {
  try {
    const data = (await response.json()) as ApiErrorResponse
    return data.message || 'Failed to search symbols'
  } catch {
    return 'Failed to search symbols'
  }
}
