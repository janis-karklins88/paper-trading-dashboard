import type {
  ApiErrorResponse,
  AuthCredentials,
  AuthMode,
  AuthResponse,
  UserResponse,
} from '../types/auth'

const AUTH_TOKEN_KEY = 'paper-trading-dashboard-token'

export function getStoredAuthToken() {
  return localStorage.getItem(AUTH_TOKEN_KEY)
}

export function storeAuthToken(token: string) {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
}

export function clearAuthToken() {
  localStorage.removeItem(AUTH_TOKEN_KEY)
}

export async function submitAuthRequest(
  mode: AuthMode,
  credentials: AuthCredentials,
) {
  const response = await fetch(`/api/users/${mode}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(credentials),
  })

  if (!response.ok) {
    throw new Error(await getErrorMessage(response))
  }

  return (await response.json()) as AuthResponse
}

export async function getCurrentUser() {
  const token = getStoredAuthToken()

  if (!token) {
    throw new Error('Missing auth token')
  }

  const response = await fetch('/api/users/me', {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  if (!response.ok) {
    throw new Error(await getErrorMessage(response))
  }

  return (await response.json()) as UserResponse
}

async function getErrorMessage(response: Response) {
  try {
    const data = (await response.json()) as ApiErrorResponse
    return data.message || 'Authentication failed'
  } catch {
    return 'Authentication failed'
  }
}
