import {
  ChartLine,
  CheckCircle2,
  Info,
  LayoutGrid,
  LogOut,
  X,
  XCircle,
  type LucideIcon,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { clearAuthToken, getCurrentUser } from '../auth/authApi'
import type { OrderResponse } from '../features/trade/api/orderApi'
import { subscribeToOrderUpdates } from '../features/trade/api/orderStream'
import type { UserResponse } from '../types/auth'

const navItems: Array<{ to: string; label: string; icon: LucideIcon }> = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutGrid },
  { to: '/trade', label: 'Trade', icon: ChartLine },
]

type ToastTone = 'success' | 'danger' | 'neutral'

type ToastMessage = {
  id: string
  message: string
  title: string
  tone: ToastTone
}

export function AppLayout() {
  const navigate = useNavigate()
  const [user, setUser] = useState<UserResponse | null>(null)
  const [toasts, setToasts] = useState<ToastMessage[]>([])
  const seenOrderStatusesRef = useRef(new Map<string, string>())
  const toastTimeoutsRef = useRef<number[]>([])

  useEffect(() => {
    let isMounted = true

    getCurrentUser()
      .then((currentUser) => {
        if (isMounted) {
          setUser(currentUser)
        }
      })
      .catch(() => {
        if (isMounted) {
          setUser(null)
        }
      })

    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    if (!user?.id) {
      return
    }

    return subscribeToOrderUpdates(user.id, (orderUpdate) => {
      const previousStatus = seenOrderStatusesRef.current.get(orderUpdate.id)

      if (previousStatus === orderUpdate.status) {
        return
      }

      seenOrderStatusesRef.current.set(orderUpdate.id, orderUpdate.status)

      const toast = buildOrderToast(orderUpdate)

      if (toast) {
        addToast(toast)
      }
    })
  }, [user?.id])

  useEffect(() => {
    return () => {
      toastTimeoutsRef.current.forEach(window.clearTimeout)
      toastTimeoutsRef.current = []
    }
  }, [])

  function handleSignOut() {
    clearAuthToken()
    navigate('/login', { replace: true })
  }

  function addToast(toast: Omit<ToastMessage, 'id'>) {
    const id = `${Date.now()}-${Math.random().toString(16).slice(2)}`
    const nextToast = { ...toast, id }

    setToasts((currentToasts) => [nextToast, ...currentToasts].slice(0, 4))

    const timeoutId = window.setTimeout(() => {
      dismissToast(id)
    }, 6000)
    toastTimeoutsRef.current.push(timeoutId)
  }

  function dismissToast(toastId: string) {
    setToasts((currentToasts) =>
      currentToasts.filter((toast) => toast.id !== toastId),
    )
  }

  return (
    <div className="grid min-h-screen grid-cols-1 bg-[#0b1322] text-[#f7fbff] md:grid-cols-[16rem_minmax(0,1fr)]">
      <aside className="flex w-full min-w-0 max-w-full flex-col overflow-hidden border-b border-[#1e293b] bg-[#0b1220] p-4 md:sticky md:top-0 md:h-screen md:border-r md:border-b-0">
        <p className="mb-4.5 text-xs font-medium text-[#a8b3cf] uppercase">
          Menu
        </p>

        <nav
          className="grid grid-cols-2 gap-1.5 md:grid-cols-1"
          aria-label="Main navigation"
        >
          {navItems.map((item) => {
            const Icon = item.icon

            return (
              <NavLink
                className={({ isActive }) =>
                  [
                    'flex min-h-10 min-w-0 items-center gap-3 rounded-md px-4 text-sm font-semibold no-underline transition',
                    isActive
                      ? 'bg-[#18234d] text-[#7592ff]'
                      : 'text-[#eef4ff] hover:bg-[#111b31] hover:text-[#7592ff]',
                  ].join(' ')
                }
                key={item.to}
                to={item.to}
              >
                {({ isActive }) => (
                  <>
                    <Icon
                      aria-hidden="true"
                      className={[
                        'h-5 w-5 shrink-0 transition',
                        isActive ? 'text-[#7592ff]' : 'text-[#98a6bf]',
                      ].join(' ')}
                      strokeWidth={1.8}
                    />
                    <span className="min-w-0 truncate">{item.label}</span>
                  </>
                )}
              </NavLink>
            )
          })}
        </nav>

        <div className="mt-4 grid min-w-0 max-w-full gap-3 md:mt-auto">
          <div className="flex min-w-0 max-w-full items-center gap-3 overflow-hidden rounded-md border border-[#1e293b] bg-[#0f1727] p-3">
            <div className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-[#18234d] text-sm font-bold text-[#8fa2ff]">
              {getUserInitials(user)}
            </div>
            <div className="min-w-0">
              <p className="mb-0.5 text-xs font-medium text-[#a8b3cf]">
                Welcome
              </p>
              <p className="truncate text-sm font-semibold text-[#eef4ff]">
                {user?.email ?? 'Loading user'}
              </p>
            </div>
          </div>

          <button
            className="flex min-h-10 w-full min-w-0 items-center justify-center gap-2 overflow-hidden rounded-md border border-[#1e293b] bg-[#0f1727] px-3 text-sm font-semibold text-[#dce8ff] hover:border-[#334666] hover:bg-[#131e31]"
            onClick={handleSignOut}
            type="button"
          >
            <LogOut aria-hidden="true" size={18} strokeWidth={1.8} />
            <span className="min-w-0 truncate">Sign out</span>
          </button>
        </div>
      </aside>

      <main className="min-w-0 p-5.5 md:p-8">
        <Outlet />
      </main>

      <ToastStack onDismiss={dismissToast} toasts={toasts} />
    </div>
  )
}

function ToastStack({
  onDismiss,
  toasts,
}: {
  onDismiss: (toastId: string) => void
  toasts: ToastMessage[]
}) {
  if (toasts.length === 0) {
    return null
  }

  return (
    <div className="fixed right-4 bottom-4 z-50 grid w-[min(26rem,calc(100vw-2rem))] gap-3">
      {toasts.map((toast) => {
        const Icon = toastIcon(toast.tone)

        return (
          <div
            className={[
              'grid grid-cols-[auto_minmax(0,1fr)_auto] gap-3 rounded-lg border bg-[#121b2d] p-4 shadow-[0_18px_50px_rgba(3,8,20,0.35)]',
              toastClass(toast.tone),
            ].join(' ')}
            key={toast.id}
          >
            <Icon
              aria-hidden="true"
              className="mt-0.5 h-5 w-5 shrink-0"
              strokeWidth={1.9}
            />
            <div className="min-w-0">
              <p className="truncate text-sm font-black text-[#f7fbff]">
                {toast.title}
              </p>
              <p className="mt-1 text-sm leading-snug text-[#b9c7df]">
                {toast.message}
              </p>
            </div>
            <button
              aria-label="Dismiss notification"
              className="grid h-7 w-7 shrink-0 place-items-center rounded-md text-[#9db2d0] hover:bg-white/8 hover:text-white"
              onClick={() => onDismiss(toast.id)}
              type="button"
            >
              <X aria-hidden="true" size={16} strokeWidth={2} />
            </button>
          </div>
        )
      })}
    </div>
  )
}

function buildOrderToast(order: OrderResponse): Omit<ToastMessage, 'id'> | null {
  if (order.status === 'FILLED') {
    return {
      title: 'Order filled',
      message: `${order.side} ${order.type.toLowerCase()} order for ${order.symbol} filled${formatExecutionPrice(order)}.`,
      tone: 'success',
    }
  }

  if (order.status === 'REJECTED') {
    return {
      title: 'Order rejected',
      message: order.rejectReason || `${order.symbol} order was rejected.`,
      tone: 'danger',
    }
  }

  if (order.status === 'CANCELED') {
    return {
      title: 'Order canceled',
      message: `${order.type} order for ${order.symbol} was canceled.`,
      tone: 'neutral',
    }
  }

  return null
}

function formatExecutionPrice(order: OrderResponse) {
  const price = Number(order.executionPrice)

  if (!Number.isFinite(price) || price <= 0) {
    return ''
  }

  return ` at ${price.toLocaleString('en-US', {
    maximumFractionDigits: 8,
  })}`
}

function toastIcon(tone: ToastTone) {
  if (tone === 'success') {
    return CheckCircle2
  }

  if (tone === 'danger') {
    return XCircle
  }

  return Info
}

function toastClass(tone: ToastTone) {
  if (tone === 'success') {
    return 'border-[#00d084]/35 text-[#00d084]'
  }

  if (tone === 'danger') {
    return 'border-[#ff5367]/35 text-[#ff8b98]'
  }

  return 'border-[#334666] text-[#9fb2ff]'
}

function getUserInitials(user: UserResponse | null) {
  if (!user?.email) {
    return 'U'
  }

  return user.email.slice(0, 1).toUpperCase()
}
