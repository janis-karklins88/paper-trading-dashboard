import { useEffect, useMemo, useState } from 'react'
import type { TradingAccountResponse } from '../trade/api/accountApi'
import { getOrders, type OrderResponse } from '../trade/api/orderApi'
import { subscribeToOrderUpdates } from '../trade/api/orderStream'
import {
  getOpenPositions,
  type PositionResponse,
} from '../trade/api/positionApi'
import { subscribeToPositionUpdates } from '../trade/api/positionStream'
import { formatMoney, formatSignedMoney } from '../../utils/formatters'

const OPEN_POSITION_FETCH_SIZE = 100
const RECENT_ROW_LIMIT = 5
const ACTIVITY_REFRESH_MS = 60_000
const allocationColors = [
  '#7fa0ff',
  '#00d084',
  '#ffb454',
  '#ff5367',
  '#b38cff',
  '#44c7f4',
  '#f08bd2',
]

type DashboardActivityProps = {
  account: TradingAccountResponse | null
  userId: string
}

type AllocationItem = {
  color: string
  label: string
  percentage: number
  value: number
}

export function DashboardActivity({ account, userId }: DashboardActivityProps) {
  const [openPositions, setOpenPositions] = useState<PositionResponse[]>([])
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [positionsError, setPositionsError] = useState('')
  const [ordersError, setOrdersError] = useState('')

  const recentPositions = useMemo(
    () => sortOpenPositions(openPositions).slice(0, RECENT_ROW_LIMIT),
    [openPositions],
  )
  const recentOrders = useMemo(
    () => sortOrders(orders).slice(0, RECENT_ROW_LIMIT),
    [orders],
  )
  const allocation = useMemo(
    () => buildAllocation(account, openPositions),
    [account, openPositions],
  )

  useEffect(() => {
    loadOpenPositions()
    loadRecentOrders()

    const intervalId = window.setInterval(() => {
      loadOpenPositions()
      loadRecentOrders()
    }, ACTIVITY_REFRESH_MS)

    return () => window.clearInterval(intervalId)

    function loadOpenPositions() {
      getOpenPositions(0, OPEN_POSITION_FETCH_SIZE)
        .then((page) => {
          setOpenPositions(sortOpenPositions(page.content))
          setPositionsError('')
        })
        .catch(() => {
          setPositionsError('Failed to load open positions')
        })
    }

    function loadRecentOrders() {
      getOrders(0, RECENT_ROW_LIMIT)
        .then((page) => {
          setOrders(sortOrders(page.content))
          setOrdersError('')
        })
        .catch(() => {
          setOrdersError('Failed to load recent orders')
        })
    }
  }, [])

  useEffect(() => {
    if (!userId) {
      return
    }

    return subscribeToPositionUpdates(userId, (positionUpdate) => {
      setOpenPositions((currentPositions) =>
        patchOpenPosition(currentPositions, positionUpdate),
      )
      setPositionsError('')
    })
  }, [userId])

  useEffect(() => {
    if (!userId) {
      return
    }

    return subscribeToOrderUpdates(userId, (orderUpdate) => {
      setOrders((currentOrders) =>
        patchOrder(currentOrders, orderUpdate).slice(0, RECENT_ROW_LIMIT),
      )
      setOrdersError('')
    })
  }, [userId])

  return (
    <section className="grid gap-4 xl:grid-cols-12">
      <div className="grid min-w-0 gap-4 xl:col-span-9">
        <RecentPositionsTable
          error={positionsError}
          positions={recentPositions}
        />
        <RecentOrdersTable error={ordersError} orders={recentOrders} />
      </div>

      <div className="min-w-0 xl:col-span-3">
        <AllocationDonut allocation={allocation} />
      </div>
    </section>
  )
}

function RecentPositionsTable({
  error,
  positions,
}: {
  error: string
  positions: PositionResponse[]
}) {
  return (
    <section className="rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.16)]">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-base font-bold text-[#f7fbff]">
            Recent open positions
          </h2>
          <p className="mt-1 text-sm text-[#9db2d0]">
            Latest five open positions by entry time
          </p>
        </div>
      </div>

      {error && <PanelError message={error} />}

      <div className="overflow-x-auto">
        <table className="w-full min-w-160 border-collapse text-left">
          <thead>
            <tr className="border-b border-[#21304a] text-xs font-bold uppercase text-[#6f829f]">
              <th className="py-2 pr-4">Symbol</th>
              <th className="py-2 pr-4">Side</th>
              <th className="py-2 pr-4">Status</th>
              <th className="py-2 pr-4 text-right">Margin</th>
              <th className="py-2 pr-4 text-right">Notional</th>
              <th className="py-2 text-right">PnL</th>
            </tr>
          </thead>
          <tbody>
            {positions.map((position) => {
              const pnl = Number(position.unrealizedPnl)

              return (
                <tr
                  className="border-b border-[#182338] last:border-b-0"
                  key={position.id}
                >
                  <td className="py-3 pr-4 text-sm font-black text-[#f7fbff]">
                    {position.symbol}
                  </td>
                  <td className="py-3 pr-4">
                    <Badge tone={position.side === 'LONG' ? 'buy' : 'sell'}>
                      {position.side}
                    </Badge>
                  </td>
                  <td className="py-3 pr-4">
                    <Badge tone="neutral">{position.status}</Badge>
                  </td>
                  <td className="py-3 pr-4 text-right text-sm font-bold text-[#eef4ff]">
                    {formatMoney(position.marginUsed)}
                  </td>
                  <td className="py-3 pr-4 text-right text-sm font-bold text-[#eef4ff]">
                    {formatMoney(getPositionNotionalValue(position))}
                  </td>
                  <td
                    className={[
                      'py-3 text-right text-sm font-black',
                      pnlClass(pnl),
                    ].join(' ')}
                  >
                    {formatSignedMoney(position.unrealizedPnl)}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {!error && positions.length === 0 && (
        <p className="py-8 text-center text-sm font-semibold text-[#9db2d0]">
          No open positions
        </p>
      )}
    </section>
  )
}

function RecentOrdersTable({
  error,
  orders,
}: {
  error: string
  orders: OrderResponse[]
}) {
  return (
    <section className="rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.16)]">
      <div className="mb-4">
        <h2 className="text-base font-bold text-[#f7fbff]">Recent orders</h2>
        <p className="mt-1 text-sm text-[#9db2d0]">
          Latest five submitted orders
        </p>
      </div>

      {error && <PanelError message={error} />}

      <div className="overflow-x-auto">
        <table className="w-full min-w-230 border-collapse text-left">
          <thead>
            <tr className="border-b border-[#21304a] text-xs font-bold uppercase text-[#6f829f]">
              <th className="py-2 pr-4">Symbol</th>
              <th className="py-2 pr-4">Type</th>
              <th className="py-2 pr-4">Status</th>
              <th className="py-2 pr-4 text-right">Leverage</th>
              <th className="py-2 pr-4 text-right">Margin</th>
              <th className="py-2 pr-4 text-right">Notional</th>
              <th className="py-2 pr-4">Placed</th>
              <th className="py-2">Filled</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr
                className="border-b border-[#182338] last:border-b-0"
                key={order.id}
              >
                <td className="py-3 pr-4 text-sm font-black text-[#f7fbff]">
                  {order.symbol}
                </td>
                <td className="py-3 pr-4">
                  <Badge tone={order.type === 'MARKET' ? 'neutral' : 'limit'}>
                    {order.type}
                  </Badge>
                </td>
                <td className="py-3 pr-4">
                  <Badge tone={orderStatusTone(order.status)}>
                    {order.status}
                  </Badge>
                </td>
                <td className="py-3 pr-4 text-right text-sm font-bold text-[#eef4ff]">
                  {formatLeverage(order.leverage)}
                </td>
                <td className="py-3 pr-4 text-right text-sm font-bold text-[#eef4ff]">
                  {formatMoney(order.marginAmount)}
                </td>
                <td className="py-3 pr-4 text-right text-sm font-bold text-[#eef4ff]">
                  {formatMoney(order.notionalValue)}
                </td>
                <td className="py-3 pr-4 text-sm font-semibold text-[#9db2d0]">
                  {formatDateTime(order.createdAt)}
                </td>
                <td className="py-3 text-sm font-semibold text-[#9db2d0]">
                  {formatDateTime(order.filledAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {!error && orders.length === 0 && (
        <p className="py-8 text-center text-sm font-semibold text-[#9db2d0]">
          No orders yet
        </p>
      )}
    </section>
  )
}

function AllocationDonut({ allocation }: { allocation: AllocationItem[] }) {
  const totalValue = allocation.reduce((sum, item) => sum + item.value, 0)

  return (
    <section className="h-full rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.16)]">
      <div className="mb-5">
        <h2 className="text-base font-bold text-[#f7fbff]">
          Asset allocation
        </h2>
        <p className="mt-1 text-sm text-[#9db2d0]">
          Cash and open-position value
        </p>
      </div>

      <div className="grid justify-items-center gap-5">
        <div
          className="relative grid aspect-square w-full max-w-54 place-items-center rounded-full"
          style={{ background: buildDonutGradient(allocation) }}
        >
          <div className="grid h-[62%] w-[62%] place-items-center rounded-full bg-[#121b2d] text-center">
            <div>
              <p className="text-xs font-bold uppercase text-[#6f829f]">
                Total
              </p>
              <p className="text-lg font-black text-[#f7fbff]">
                {formatMoney(totalValue)}
              </p>
            </div>
          </div>
        </div>

        <div className="grid w-full gap-3">
          {allocation.map((item) => (
            <div
              className="grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-2"
              key={item.label}
            >
              <span
                className="h-2.5 w-2.5 rounded-full"
                style={{ backgroundColor: item.color }}
              />
              <span className="truncate text-sm font-bold text-[#eef4ff]">
                {item.label}
              </span>
              <span className="text-sm font-black text-[#9db2d0]">
                {formatPercent(item.percentage)}
              </span>
            </div>
          ))}
        </div>
      </div>

      {allocation.length === 0 && (
        <p className="py-10 text-center text-sm font-semibold text-[#9db2d0]">
          No allocation data
        </p>
      )}
    </section>
  )
}

function buildAllocation(
  account: TradingAccountResponse | null,
  positions: PositionResponse[],
) {
  const groupedValues = new Map<string, number>()
  const cashValue = Math.max(Number(account?.cashBalance ?? 0), 0)

  if (cashValue > 0) {
    groupedValues.set('Cash', cashValue)
  }

  for (const position of positions) {
    const value = Math.max(getPositionValue(position), 0)
    if (value <= 0) {
      continue
    }

    groupedValues.set(position.symbol, (groupedValues.get(position.symbol) ?? 0) + value)
  }

  const total = [...groupedValues.values()].reduce((sum, value) => sum + value, 0)

  if (total <= 0) {
    return []
  }

  return [...groupedValues.entries()]
    .sort((left, right) => right[1] - left[1])
    .map(([label, value], index) => ({
      color: allocationColors[index % allocationColors.length],
      label,
      percentage: (value / total) * 100,
      value,
    }))
}

function buildDonutGradient(allocation: AllocationItem[]) {
  if (allocation.length === 0) {
    return '#1e293b'
  }

  let cursor = 0
  const segments = allocation.map((item) => {
    const start = cursor
    cursor += item.percentage
    return `${item.color} ${start}% ${cursor}%`
  })

  return `conic-gradient(${segments.join(', ')})`
}

function patchOpenPosition(
  positions: PositionResponse[],
  positionUpdate: PositionResponse,
) {
  const withoutPosition = positions.filter(
    (position) => position.id !== positionUpdate.id,
  )

  if (positionUpdate.status !== 'OPEN') {
    return sortOpenPositions(withoutPosition).slice(0, OPEN_POSITION_FETCH_SIZE)
  }

  return sortOpenPositions([positionUpdate, ...withoutPosition]).slice(
    0,
    OPEN_POSITION_FETCH_SIZE,
  )
}

function patchOrder(orders: OrderResponse[], orderUpdate: OrderResponse) {
  const withoutOrder = orders.filter((order) => order.id !== orderUpdate.id)
  return sortOrders([orderUpdate, ...withoutOrder])
}

function sortOpenPositions(positions: PositionResponse[]) {
  return [...positions].sort(
    (left, right) =>
      new Date(right.openedAt).getTime() - new Date(left.openedAt).getTime(),
  )
}

function sortOrders(orders: OrderResponse[]) {
  return [...orders].sort(
    (left, right) =>
      new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime(),
  )
}

function getPositionValue(position: PositionResponse) {
  return Number(position.marginUsed) + Number(position.unrealizedPnl)
}

function getPositionNotionalValue(position: PositionResponse) {
  return Number(position.marginUsed) * Number(position.leverage)
}

function Badge({
  children,
  tone,
}: {
  children: string
  tone: 'buy' | 'sell' | 'neutral' | 'limit' | 'success' | 'danger'
}) {
  return (
    <span
      className={[
        'inline-flex min-h-7 items-center rounded px-2 text-xs font-black',
        badgeClass(tone),
      ].join(' ')}
    >
      {children}
    </span>
  )
}

function badgeClass(tone: 'buy' | 'sell' | 'neutral' | 'limit' | 'success' | 'danger') {
  if (tone === 'buy' || tone === 'success') {
    return 'bg-[#00d084]/12 text-[#00d084]'
  }

  if (tone === 'sell' || tone === 'danger') {
    return 'bg-[#ff5367]/12 text-[#ff8b98]'
  }

  if (tone === 'limit') {
    return 'bg-[#ffb454]/12 text-[#ffc978]'
  }

  return 'bg-[#18234d] text-[#9fb2ff]'
}

function orderStatusTone(status: string): 'neutral' | 'success' | 'danger' {
  if (status === 'FILLED') {
    return 'success'
  }

  if (status === 'REJECTED' || status === 'CANCELED') {
    return 'danger'
  }

  return 'neutral'
}

function pnlClass(value: number) {
  if (value > 0) {
    return 'text-[#00d084]'
  }

  if (value < 0) {
    return 'text-[#ff8b98]'
  }

  return 'text-[#eef4ff]'
}

function formatLeverage(value: string | number) {
  const numericValue = Number(value)

  if (!Number.isFinite(numericValue)) {
    return '--'
  }

  return `${Number.isInteger(numericValue) ? numericValue : numericValue.toFixed(2)}x`
}

function formatDateTime(value: string | null) {
  if (!value) {
    return '--'
  }

  const date = new Date(value)

  if (!Number.isFinite(date.getTime())) {
    return '--'
  }

  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    hour: '2-digit',
    hour12: false,
    minute: '2-digit',
    month: 'short',
  }).format(date)
}

function formatPercent(value: number) {
  if (!Number.isFinite(value)) {
    return '0.0%'
  }

  return `${value.toFixed(1)}%`
}

function PanelError({ message }: { message: string }) {
  return (
    <p className="mb-3 rounded-md border border-[#ff5367]/30 bg-[#ff5367]/12 px-3 py-2 text-sm font-bold text-[#ffdce1]">
      {message}
    </p>
  )
}
