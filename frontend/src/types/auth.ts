export type AuthMode = 'login' | 'register'

export type AuthCredentials = {
  email: string
  password: string
}

export type AuthResponse = {
  token: string
}

export type UserResponse = {
  id: string
  email: string
}

export type ApiErrorResponse = {
  message?: string
}
