import {
  ChartLine,
  LayoutGrid,
  LogOut,
  type LucideIcon,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { clearAuthToken, getCurrentUser } from '../auth/authApi'
import type { UserResponse } from '../types/auth'

const navItems: Array<{ to: string; label: string; icon: LucideIcon }> = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutGrid },
  { to: '/trade', label: 'Trade', icon: ChartLine },
]

export function AppLayout() {
  const navigate = useNavigate()
  const [user, setUser] = useState<UserResponse | null>(null)

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

  function handleSignOut() {
    clearAuthToken()
    navigate('/login', { replace: true })
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
    </div>
  )
}

function getUserInitials(user: UserResponse | null) {
  if (!user?.email) {
    return 'U'
  }

  return user.email.slice(0, 1).toUpperCase()
}
