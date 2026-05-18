import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { AuthPage } from '../auth/AuthPage'
import {
  getStoredAuthToken,
  storeAuthToken,
  submitAuthRequest,
} from '../auth/authApi'

const labelClass = 'grid gap-2 text-sm font-extrabold text-[#dce8ff]'
const inputClass =
  'min-h-11.5 w-full rounded-md border border-[#21304a] bg-[#0d1627] px-3.5 text-[#f7fbff] outline-none transition placeholder:text-[#6f829f] focus:border-[#6f84ff] focus:shadow-[0_0_0_4px_rgba(76,102,255,0.16)]'
const primaryActionClass =
  'min-h-11.5 w-full rounded-md border border-[#5a70ff] bg-[#4c66ff] font-black text-white shadow-[0_14px_28px_rgba(76,102,255,0.24)] hover:bg-[#5b73ff] disabled:cursor-not-allowed disabled:opacity-70'

export function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (getStoredAuthToken()) {
    return <Navigate to="/dashboard" replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    try {
      const response = await submitAuthRequest('login', { email, password })
      storeAuthToken(response.token)
      navigate('/dashboard', { replace: true })
    } catch (caughtError) {
      setError(
        caughtError instanceof Error ? caughtError.message : 'Login failed',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthPage
      title="Login"

    >
      <form className="grid gap-4" onSubmit={handleSubmit}>
        <label className={labelClass}>
          Email
          <input
            autoComplete="email"
            className={inputClass}
            name="email"
            onChange={(event) => setEmail(event.target.value)}
            placeholder="name@example.com"
            required
            type="email"
            value={email}
          />
        </label>

        <label className={labelClass}>
          Password
          <input
            autoComplete="current-password"
            className={inputClass}
            name="password"
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Enter password"
            required
            type="password"
            value={password}
          />
        </label>

        {error && (
          <p className="-mt-0.5 rounded-md bg-[#ff5367]/12 px-3 py-2.5 text-sm font-bold text-[#ffdce1]">
            {error}
          </p>
        )}

        <button
          className={primaryActionClass}
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? 'Logging in' : 'Login'}
        </button>
      </form>

      <p className="mt-4.5 text-center text-sm leading-relaxed text-[#9db2d0]">
        Do not have an account?{' '}
        <Link
          className="font-black text-[#8fa2ff] no-underline hover:text-white"
          to="/register"
        >
          Register
        </Link>
      </p>
    </AuthPage>
  )
}
