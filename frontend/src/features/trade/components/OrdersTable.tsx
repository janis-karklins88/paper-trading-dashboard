import { useCallback, useEffect, useState } from 'react'
import { cancelOrder, getOrders, type OrderResponse } from '../api/orderApi'

const ORDER_REFRESH_MS = 15_000

type OrdersTableProps = {
  refreshKey?: number
}

export function OrdersTable({ refreshKey = 0 }: OrdersTableProps) {
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [cancelingOrderId, setCancelingOrderId] = useState('')
  const [error, setError] = useState('')

  const loadOrders = useCallback(() => {
    getOrders()
      .then((nextOrders) => {
        setOrders(nextOrders)
        setError('')
      })
      .catch(() => {
        setError('Failed to load orders')
      })
      .finally(() => {
        setIsLoading(false)
      })
  }, [])

  useEffect(() => {
    loadOrders()

    // Temporary polling until order websocket updates or shared trade state exist.
    const intervalId = window.setInterval(loadOrders, ORDER_REFRESH_MS)

    return () => {
      window.clearInterval(intervalId)
    }
  }, [loadOrders])

  useEffect(() => {
    if (refreshKey > 0) {
      loadOrders()
    }
  }, [loadOrders, refreshKey])

  async function handleCancelOrder(orderId: string) {
    setCancelingOrderId(orderId)
    setError('')

    try {
      await cancelOrder(orderId)
      await loadOrders()
    } catch (caughtError) {
      setError(
        caughtError instanceof Error
          ? caughtError.message
          : 'Failed to cancel order',
      )
    } finally {
      setCancelingOrderId('')
    }
  }

  return (
    <section className="min-w-0 rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.22)]">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-xl font-bold">Orders</h2>
        <span className="text-xs font-semibold text-[#9db2d0]">
          {orders.length} total
        </span>
      </div>

      <div className="max-w-full overflow-x-auto rounded-md border border-[#1e293b]">
        <table className="w-full min-w-225 border-collapse text-left text-sm">
          <thead className="bg-[#0f1727] text-[#9db2d0]">
            <tr>
              <th className={headerCellClass}>Symbol</th>
              <th className={headerCellClass}>Side</th>
              <th className={headerCellClass}>Type</th>
              <th className={headerCellClass}>Status</th>
              <th className={headerCellClass}>Leverage</th>
              <th className={headerCellClass}>Margin</th>
              <th className={headerCellClass}>Notional</th>
              <th className={headerCellClass}>Placed</th>
              <th className={headerCellClass}>Filled</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => {
              const canCancelOrder =
                order.type === 'LIMIT' &&
                (order.status === 'OPEN' || order.status === 'PENDING')
              const isCanceling = cancelingOrderId === order.id

              return (
                <tr className="border-t border-[#1e293b]" key={order.id}>
                  <td className="px-4 py-3 font-bold text-[#f7fbff]">
                    {order.symbol}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={[
                        'font-bold',
                        order.side === 'BUY'
                          ? 'text-[#00d084]'
                          : 'text-[#ff5367]',
                      ].join(' ')}
                    >
                      {formatEnumLabel(order.side)}
                    </span>
                  </td>
                  <td className={bodyCellClass}>
                    {formatEnumLabel(order.type)}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={[
                        'rounded-full px-2.5 py-1 text-xs font-bold',
                        statusClass(order.status),
                      ].join(' ')}
                    >
                      {formatEnumLabel(order.status)}
                    </span>
                  </td>
                  <td className={bodyCellClass}>
                    {formatLeverage(order.leverage)}
                  </td>
                  <td className={bodyCellClass}>
                    {formatMoney(order.marginAmount)}
                  </td>
                  <td className={bodyCellClass}>
                    {formatMoney(order.notionalValue)}
                  </td>
                  <td className={bodyCellClass}>
                    {formatDateTime(order.createdAt)}
                  </td>
                  <td className={bodyCellClass}>
                    {canCancelOrder ? (
                      <button
                        className="min-h-8 rounded-md border border-[#ff5367]/30 bg-[#ff5367]/12 px-3 text-xs font-bold text-[#ffdce1] transition hover:bg-[#ff5367]/20 disabled:cursor-not-allowed disabled:opacity-60"
                        disabled={isCanceling}
                        onClick={() => void handleCancelOrder(order.id)}
                        type="button"
                      >
                        {isCanceling ? 'Canceling' : 'Cancel'}
                      </button>
                    ) : (
                      filledColumnText(order)
                    )}
                  </td>
                </tr>
              )
            })}

            {!isLoading && orders.length === 0 && (
              <tr className="border-t border-[#1e293b]">
                <td className="px-4 py-8 text-center text-[#9db2d0]" colSpan={9}>
                  No orders yet
                </td>
              </tr>
            )}
          </tbody>
        </table>

        {isLoading && (
          <div className="border-t border-[#1e293b] px-4 py-8 text-center text-sm font-semibold text-[#9db2d0]">
            Loading orders
          </div>
        )}

        {error && (
          <div className="border-t border-[#1e293b] bg-[#ff5367]/12 px-4 py-3 text-sm font-bold text-[#ffdce1]">
            {error}
          </div>
        )}
      </div>
    </section>
  )
}

function formatEnumLabel(value: string) {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

function formatLeverage(value: string | number) {
  const numericValue = Number(value)

  if (!Number.isFinite(numericValue)) {
    return '--'
  }

  return `${numericValue}x`
}

function formatMoney(value: string | number | null) {
  const numericValue = Number(value)

  if (!Number.isFinite(numericValue)) {
    return '--'
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numericValue)
}

function formatDateTime(value: string | null) {
  if (!value) {
    return '--'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return '--'
  }

  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

function filledColumnText(order: OrderResponse) {
  if (order.status === 'FILLED') {
    return formatDateTime(order.filledAt)
  }

  return formatEnumLabel(order.status)
}

function statusClass(status: string) {
  switch (status) {
    case 'FILLED':
      return 'bg-[#00d084]/12 text-[#00d084]'
    case 'OPEN':
    case 'PENDING':
      return 'bg-[#7592ff]/14 text-[#9bb0ff]'
    case 'CANCELED':
      return 'bg-[#9db2d0]/12 text-[#9db2d0]'
    case 'REJECTED':
      return 'bg-[#ff5367]/12 text-[#ffdce1]'
    default:
      return 'bg-[#1e293b] text-[#9db2d0]'
  }
}

const headerCellClass =
  'whitespace-nowrap px-4 py-3 text-xs font-extrabold uppercase'
const bodyCellClass = 'whitespace-nowrap px-4 py-3 text-[#9db2d0]'
