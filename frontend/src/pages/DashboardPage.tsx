import {
  Banknote,
  CircleDollarSign,
  Landmark,
  LineChart,
  TrendingDown,
  TrendingUp,
  Wallet,
  type LucideIcon,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { getCurrentUser } from '../auth/authApi'
import {
  getTradingAccount,
  type TradingAccountResponse,
} from '../features/trade/api/accountApi'
import { subscribeToTradingAccountUpdates } from '../features/trade/api/accountStream'
import { DashboardActivity } from '../features/dashboard/DashboardActivity'
import { EquityCurveChart } from '../features/dashboard/EquityCurveChart'
import { formatMoney, formatSignedMoney } from '../utils/formatters'

export function DashboardPage() {
  const { account, error, isLoading, userId } = useDashboardAccount()
  const accountCards = useMemo(() => buildAccountCards(account), [account])

  return (
    <section className="grid gap-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="mb-2 text-xs font-extrabold uppercase text-[#a9c7ff]">
            Overview
          </p>
          <h1 className="mb-0 text-[34px] leading-none font-bold">
            Dashboard
          </h1>
        </div>
        <div className="inline-flex w-fit items-center gap-2 rounded-md border border-[#21304a] bg-[#0f1727] px-3 py-2 text-xs font-bold text-[#9db2d0]">
          <span
            className={[
              'h-2 w-2 rounded-full',
              account ? 'bg-[#00d084]' : 'bg-[#6f829f]',
            ].join(' ')}
          />
          {account ? 'Live' : isLoading ? 'Loading' : 'Offline'}
        </div>
      </div>

      {error && (
        <p className="rounded-md border border-[#ff5367]/30 bg-[#ff5367]/12 px-4 py-3 text-sm font-bold text-[#ffdce1]">
          {error}
        </p>
      )}

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
        {accountCards.map((card) => (
          <AccountCard
            icon={card.icon}
            key={card.label}
            label={card.label}
            tone={card.tone}
            value={card.value}
          />
        ))}
      </div>

      <EquityCurveChart />

      <DashboardActivity account={account} userId={userId} />
    </section>
  )
}

function useDashboardAccount() {
  const [userId, setUserId] = useState('')
  const [account, setAccount] = useState<TradingAccountResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let isMounted = true

    getCurrentUser()
      .then((user) => {
        if (isMounted) {
          setUserId(user.id)
        }
      })
      .catch(() => {
        if (isMounted) {
          setError('Failed to load current user')
        }
      })

    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    let isMounted = true

    getTradingAccount()
      .then((nextAccount) => {
        if (isMounted) {
          setAccount(nextAccount)
          setError('')
        }
      })
      .catch(() => {
        if (isMounted) {
          setError('Failed to load account summary')
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false)
        }
      })

    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    if (!userId) {
      return
    }

    return subscribeToTradingAccountUpdates(userId, (nextAccount) => {
      setAccount(nextAccount)
      setError('')
      setIsLoading(false)
    })
  }, [userId])

  return { account, error, isLoading, userId }
}

type AccountCardTone = 'neutral' | 'positive' | 'negative'

type AccountCardModel = {
  icon: LucideIcon
  label: string
  tone: AccountCardTone
  value: string
}

function buildAccountCards(
  account: TradingAccountResponse | null,
): AccountCardModel[] {
  const unrealizedPnl = Number(account?.unrealizedPnl ?? 0)
  const netPnl = Number(account?.netPnl ?? 0)

  return [
    {
      icon: Wallet,
      label: 'Cash balance',
      tone: 'neutral',
      value: formatMoney(account?.cashBalance),
    },
    {
      icon: Landmark,
      label: 'Equity',
      tone: 'neutral',
      value: formatMoney(account?.equity),
    },
    {
      icon: CircleDollarSign,
      label: 'Buying power',
      tone: 'neutral',
      value: formatMoney(account?.buyingPower),
    },
    {
      icon: Banknote,
      label: 'Margin',
      tone: 'neutral',
      value: formatMoney(account?.reservedMargin),
    },
    {
      icon: unrealizedPnl < 0 ? TrendingDown : TrendingUp,
      label: 'Unrealized PnL',
      tone: pnlTone(unrealizedPnl),
      value: formatSignedMoney(account?.unrealizedPnl),
    },
    {
      icon: LineChart,
      label: 'Net PnL',
      tone: pnlTone(netPnl),
      value: formatSignedMoney(account?.netPnl),
    },
  ]
}

function AccountCard({
  icon: Icon,
  label,
  tone,
  value,
}: AccountCardModel) {
  return (
    <article className="min-h-31 rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-4 shadow-[0_18px_50px_rgba(3,8,20,0.16)]">
      <div className="mb-4 flex items-center justify-between gap-3">
        <p className="truncate text-xs font-bold uppercase text-[#9db2d0]">
          {label}
        </p>
        <span
          className={[
            'grid h-8 w-8 shrink-0 place-items-center rounded-md border',
            iconToneClass(tone),
          ].join(' ')}
        >
          <Icon aria-hidden="true" size={18} strokeWidth={1.8} />
        </span>
      </div>
      <p className={[valueClass(tone), 'truncate text-2xl font-black'].join(' ')}>
        {value}
      </p>
    </article>
  )
}

function pnlTone(value: number): AccountCardTone {
  if (value > 0) {
    return 'positive'
  }

  if (value < 0) {
    return 'negative'
  }

  return 'neutral'
}

function iconToneClass(tone: AccountCardTone) {
  if (tone === 'positive') {
    return 'border-[#00d084]/25 bg-[#00d084]/12 text-[#00d084]'
  }

  if (tone === 'negative') {
    return 'border-[#ff5367]/25 bg-[#ff5367]/12 text-[#ff8b98]'
  }

  return 'border-[#2f4164] bg-[#18234d] text-[#9fb2ff]'
}

function valueClass(tone: AccountCardTone) {
  if (tone === 'positive') {
    return 'text-[#00d084]'
  }

  if (tone === 'negative') {
    return 'text-[#ff8b98]'
  }

  return 'text-[#f7fbff]'
}
