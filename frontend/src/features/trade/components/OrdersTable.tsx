import { useCallback, useEffect, useState } from 'react'
import { cancelOrder, getOrders, type OrderResponse } from '../api/orderApi'
import { formatOptionalPrice } from '../../../utils/formatters'
import { getCurrentUser } from '../../../auth/authApi'
import { subscribeToOrderUpdates } from '../api/orderStream'

const ORDER_REFRESH_MS = 60_000
const ORDER_PAGE_SIZE = 10

type OrdersTableProps = {
  refreshKey?: number
}

export function OrdersTable({ refreshKey = 0 }: OrdersTableProps) {
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalOrders, setTotalOrders] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [cancelingOrderId, setCancelingOrderId] = useState('')
  const [error, setError] = useState('')
  const [userId, setUserId] = useState('')

  const loadOrders = useCallback(() => {
    getOrders(page, ORDER_PAGE_SIZE)
      .then((nextPage) => {
        setOrders(nextPage.content)
        setTotalPages(nextPage.totalPages)
        setTotalOrders(nextPage.totalElements)
        setError('')
      })
      .catch(() => {
        setError('Failed to load orders')
      })
      .finally(() => {
        setIsLoading(false)
      })
  }, [page])

  useEffect(() => {
    loadOrders()

    // Recovery polling; order changes arrive over websocket.
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
          setUserId('')
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

    return subscribeToOrderUpdates(userId, (orderUpdate) => {
      setOrders((currentOrders) => patchOrder(currentOrders, orderUpdate))
    })
  }, [userId])

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
          {totalOrders} total
        </span>
      </div>

      <div className="watchlist-scroll max-w-full overflow-x-auto rounded-md border border-[#1e293b]">
        <table className="w-full min-w-260 border-collapse text-left text-[13px]">
          <thead className="bg-[#0f1727] text-[#9db2d0]">
            <tr>
              <th className={headerCellClass}>Symbol</th>
              <th className={headerCellClass}>Side</th>
              <th className={headerCellClass}>Type</th>
              <th className={headerCellClass}>Status</th>
              <th className={headerCellClass}>Qty</th>
              <th className={headerCellClass}>Price</th>
              <th className={headerCellClass}>Leverage</th>
              <th className={headerCellClass}>Margin</th>
              <th className={headerCellClass}>Notional</th>
              <th className={headerCellClass}>Fee</th>
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
                  <td className="px-3 py-2.5 font-bold text-[#f7fbff]">
                    {order.symbol}
                  </td>
                  <td className="px-3 py-2.5">
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
                  <td className="px-3 py-2.5">
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
                    {formatQuantity(order.quantity)}
                  </td>
                  <td className={bodyCellClass}>
                    {formatOrderPrice(order)}
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
                    {formatMoney(order.feeAmount)}
                  </td>
                  <td className={bodyCellClass}>
                    {formatDateTime(order.createdAt)}
                  </td>
                  <td className={bodyCellClass}>
                    {canCancelOrder ? (
                      <button
                        className="min-h-7 rounded-md border border-[#ff5367]/30 bg-[#ff5367]/12 px-2.5 text-xs font-bold text-[#ffdce1] transition hover:bg-[#ff5367]/20 disabled:cursor-not-allowed disabled:opacity-60"
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
                <td className="px-4 py-8 text-center text-[#9db2d0]" colSpan={12}>
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

      <PaginationControls
        className="mt-3"
        currentPage={page}
        onNext={() =>
          setPage((currentPage) =>
            Math.min(currentPage + 1, Math.max(totalPages - 1, 0)),
          )
        }
        onPrevious={() =>
          setPage((currentPage) => Math.max(currentPage - 1, 0))
        }
        totalPages={totalPages}
      />
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

function formatQuantity(value: string | number | null) {
  const numericValue = Number(value)

  if (!Number.isFinite(numericValue)) {
    return '--'
  }

  return new Intl.NumberFormat('en-US', {
    maximumFractionDigits: 8,
  }).format(numericValue)
}

function formatOrderPrice(order: OrderResponse) {
  if (order.executionPrice !== null) {
    return formatOptionalPrice(order.executionPrice)
  }

  return formatOptionalPrice(order.limitPrice)
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

function patchOrder(orders: OrderResponse[], orderUpdate: OrderResponse) {
  const existingIndex = orders.findIndex((order) => order.id === orderUpdate.id)

  if (existingIndex === -1) {
    return sortOrders([orderUpdate, ...orders]).slice(0, ORDER_PAGE_SIZE)
  }

  const nextOrders = [...orders]
  nextOrders[existingIndex] = orderUpdate
  return sortOrders(nextOrders)
}

function sortOrders(orders: OrderResponse[]) {
  return [...orders].sort(
    (left, right) =>
      new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime(),
  )
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
  'whitespace-nowrap px-3 py-2.5 text-xs font-extrabold uppercase'
const bodyCellClass = 'whitespace-nowrap px-3 py-2.5 text-[#9db2d0]'

type PaginationControlsProps = {
  className?: string
  currentPage: number
  totalPages: number
  onPrevious: () => void
  onNext: () => void
}

function PaginationControls({
  className = '',
  currentPage,
  totalPages,
  onPrevious,
  onNext,
}: PaginationControlsProps) {
  if (totalPages <= 1) {
    return null
  }

  return (
    <div
      className={[
        'flex items-center justify-end gap-3 text-xs font-semibold text-[#9db2d0]',
        className,
      ].join(' ')}
    >
      <button
        className={paginationButtonClass}
        disabled={currentPage <= 0}
        onClick={onPrevious}
        type="button"
      >
        Previous
      </button>
      <span>
        Page {currentPage + 1} / {totalPages}
      </span>
      <button
        className={paginationButtonClass}
        disabled={currentPage >= totalPages - 1}
        onClick={onNext}
        type="button"
      >
        Next
      </button>
    </div>
  )
}

const paginationButtonClass =
  'min-h-8 rounded-md border border-[#21304a] bg-[#0f1727] px-3 text-[#dce8ff] transition hover:border-[#334666] hover:bg-[#131e31] disabled:cursor-not-allowed disabled:opacity-50'
