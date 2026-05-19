import { useCallback, useEffect, useState } from 'react'
import {
  closePosition,
  getClosedPositions,
  getOpenPositions,
  type PositionResponse,
} from '../api/positionApi'
import { formatOptionalPrice } from '../../../utils/formatters'

const POSITION_REFRESH_MS = 15_000
const POSITION_PAGE_SIZE = 10

type PositionsTableProps = {
  onPositionClosed?: () => void
}

export function PositionsTable({ onPositionClosed }: PositionsTableProps) {
  const [positions, setPositions] = useState<PositionResponse[]>([])
  const [openPage, setOpenPage] = useState(0)
  const [closedPage, setClosedPage] = useState(0)
  const [openTotalPages, setOpenTotalPages] = useState(0)
  const [closedTotalPages, setClosedTotalPages] = useState(0)
  const [openTotalPositions, setOpenTotalPositions] = useState(0)
  const [closedTotalPositions, setClosedTotalPositions] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [closingPositionId, setClosingPositionId] = useState('')
  const [error, setError] = useState('')

  const loadPositionsForPages = useCallback(
    async (nextOpenPage: number, nextClosedPage: number) => {
      const [openPositions, closedPositions] = await Promise.all([
        getOpenPositions(nextOpenPage, POSITION_PAGE_SIZE),
        getClosedPositions(nextClosedPage, POSITION_PAGE_SIZE),
      ])

      setPositions([
        ...sortOpenPositions(openPositions.content),
        ...sortClosedPositions(closedPositions.content),
      ])
      setOpenTotalPages(openPositions.totalPages)
      setClosedTotalPages(closedPositions.totalPages)
      setOpenTotalPositions(openPositions.totalElements)
      setClosedTotalPositions(closedPositions.totalElements)
      setError('')
    },
    [],
  )

  useEffect(() => {
    let isMounted = true

    const loadMountedPositions = () => {
      Promise.all([
        getOpenPositions(openPage, POSITION_PAGE_SIZE),
        getClosedPositions(closedPage, POSITION_PAGE_SIZE),
      ])
        .then(([openPositions, closedPositions]) => {
          if (isMounted) {
            setPositions([
              ...sortOpenPositions(openPositions.content),
              ...sortClosedPositions(closedPositions.content),
            ])
            setOpenTotalPages(openPositions.totalPages)
            setClosedTotalPages(closedPositions.totalPages)
            setOpenTotalPositions(openPositions.totalElements)
            setClosedTotalPositions(closedPositions.totalElements)
            setError('')
          }
        })
        .catch(() => {
          if (isMounted) {
            setError('Failed to load positions')
          }
        })
        .finally(() => {
          if (isMounted) {
            setIsLoading(false)
          }
        })
    }

    loadMountedPositions()

    // Temporary polling until position websocket updates or shared trade state exist.
    const intervalId = window.setInterval(
      loadMountedPositions,
      POSITION_REFRESH_MS,
    )

    return () => {
      isMounted = false
      window.clearInterval(intervalId)
    }
  }, [closedPage, openPage])

  async function handleClosePosition(positionId: string) {
    setClosingPositionId(positionId)
    setError('')

    try {
      await closePosition(positionId)
      setClosedPage(0)
      await loadPositionsForPages(openPage, 0)
      onPositionClosed?.()
    } catch (caughtError) {
      setError(
        caughtError instanceof Error
          ? caughtError.message
          : 'Failed to close position',
      )
    } finally {
      setClosingPositionId('')
    }
  }

  return (
    <section className="min-w-0 rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.22)]">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-xl font-bold">Positions</h2>
        <span className="text-xs font-semibold text-[#9db2d0]">
          {openTotalPositions} open / {closedTotalPositions} closed
        </span>
      </div>

      <div className="max-w-full overflow-x-auto rounded-md border border-[#1e293b]">
        <table className="w-full min-w-250 border-collapse text-left text-sm">
          <thead className="bg-[#0f1727] text-[#9db2d0]">
            <tr>
              <th className={headerCellClass}>Symbol</th>
              <th className={headerCellClass}>Side</th>
              <th className={headerCellClass}>Status</th>
              <th className={headerCellClass}>Entry price</th>
              <th className={headerCellClass}>Current price</th>
              <th className={headerCellClass}>TP</th>
              <th className={headerCellClass}>SL</th>
              <th className={headerCellClass}>PnL</th>
              <th className={headerCellClass}>Open at</th>
              <th className={headerCellClass}>Action</th>
            </tr>
          </thead>
          <tbody>
            {positions.map((position) => {
              const pnlValue = getPositionPnl(position)
              const pnl = Number(pnlValue)
              const pnlPercent = calculatePnlPercent(position)
              const isClosing = closingPositionId === position.id

              return (
                <tr className="border-t border-[#1e293b]" key={position.id}>
                  <td className="px-4 py-3 font-bold text-[#f7fbff]">
                    {position.symbol}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={[
                        'font-bold',
                        position.side === 'LONG'
                          ? 'text-[#00d084]'
                          : 'text-[#ff5367]',
                      ].join(' ')}
                    >
                      {formatEnumLabel(position.side)}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={[
                        'rounded-full px-2.5 py-1 text-xs font-bold',
                        position.status === 'OPEN'
                          ? 'bg-[#00d084]/12 text-[#00d084]'
                          : 'bg-[#9db2d0]/12 text-[#9db2d0]',
                      ].join(' ')}
                    >
                      {formatEnumLabel(position.status)}
                    </span>
                  </td>
                  <td className={bodyCellClass}>
                    {formatOptionalPrice(position.avgEntryPrice)}
                  </td>
                  <td className={bodyCellClass}>
                    {formatOptionalPrice(position.currentPrice)}
                  </td>
                  <td className={bodyCellClass}>--</td>
                  <td className={bodyCellClass}>--</td>
                  <td
                    className={[
                      'whitespace-nowrap px-4 py-3 font-bold',
                      pnl >= 0 ? 'text-[#00d084]' : 'text-[#ff5367]',
                    ].join(' ')}
                  >
                    {formatPnlMoney(pnlValue)}
                    <span className="ml-1 text-xs">
                      ({formatPercent(pnlPercent)})
                    </span>
                  </td>
                  <td className={bodyCellClass}>
                    {formatDateTime(position.openedAt)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    {position.status === 'OPEN' ? (
                      <button
                        className="min-h-8 rounded-md border border-[#ff5367]/30 bg-[#ff5367]/12 px-3 text-xs font-bold text-[#ffdce1] transition hover:bg-[#ff5367]/20 disabled:cursor-not-allowed disabled:opacity-60"
                        disabled={isClosing}
                        onClick={() => void handleClosePosition(position.id)}
                        type="button"
                      >
                        {isClosing ? 'Closing' : 'Close'}
                      </button>
                    ) : (
                      <span className="text-[#9db2d0]">
                        {formatDateTime(position.closedAt)}
                      </span>
                    )}
                  </td>
                </tr>
              )
            })}

            {!isLoading && positions.length === 0 && (
              <tr className="border-t border-[#1e293b]">
                <td
                  className="px-4 py-8 text-center text-[#9db2d0]"
                  colSpan={10}
                >
                  No positions yet
                </td>
              </tr>
            )}
          </tbody>
        </table>

        {isLoading && (
          <div className="border-t border-[#1e293b] px-4 py-8 text-center text-sm font-semibold text-[#9db2d0]">
            Loading positions
          </div>
        )}

        {error && (
          <div className="border-t border-[#1e293b] bg-[#ff5367]/12 px-4 py-3 text-sm font-bold text-[#ffdce1]">
            {error}
          </div>
        )}
      </div>

      <div className="mt-3 grid gap-2 md:grid-cols-2">
        <PaginationControls
          currentPage={openPage}
          label="Open"
          onNext={() =>
            setOpenPage((currentPage) =>
              Math.min(currentPage + 1, Math.max(openTotalPages - 1, 0)),
            )
          }
          onPrevious={() =>
            setOpenPage((currentPage) => Math.max(currentPage - 1, 0))
          }
          totalPages={openTotalPages}
        />
        <PaginationControls
          currentPage={closedPage}
          label="Closed"
          onNext={() =>
            setClosedPage((currentPage) =>
              Math.min(currentPage + 1, Math.max(closedTotalPages - 1, 0)),
            )
          }
          onPrevious={() =>
            setClosedPage((currentPage) => Math.max(currentPage - 1, 0))
          }
          totalPages={closedTotalPages}
        />
      </div>
    </section>
  )
}

function calculatePnlPercent(position: PositionResponse) {
  const pnl = Number(getPositionPnl(position))
  const margin = Number(position.marginUsed)

  if (!Number.isFinite(pnl) || !Number.isFinite(margin) || margin <= 0) {
    return null
  }

  return (pnl / margin) * 100
}

function getPositionPnl(position: PositionResponse) {
  return position.status === 'OPEN' ? position.unrealizedPnl : position.realizedPnl
}

function sortOpenPositions(positions: PositionResponse[]) {
  return [...positions].sort(
    (left, right) =>
      new Date(right.openedAt).getTime() - new Date(left.openedAt).getTime(),
  )
}

function sortClosedPositions(positions: PositionResponse[]) {
  return [...positions].sort(
    (left, right) =>
      getTimeOrZero(right.closedAt) - getTimeOrZero(left.closedAt),
  )
}

function getTimeOrZero(value: string | null) {
  return value ? new Date(value).getTime() : 0
}

function formatEnumLabel(value: string) {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

function formatPnlMoney(value: string | number | null) {
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

function formatPercent(value: number | null) {
  if (value === null || !Number.isFinite(value)) {
    return '--'
  }

  return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
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

type PaginationControlsProps = {
  currentPage: number
  label: string
  totalPages: number
  onPrevious: () => void
  onNext: () => void
}

function PaginationControls({
  currentPage,
  label,
  totalPages,
  onPrevious,
  onNext,
}: PaginationControlsProps) {
  if (totalPages <= 1) {
    return null
  }

  return (
    <div className="flex items-center justify-end gap-3 text-xs font-semibold text-[#9db2d0]">
      <button
        className={paginationButtonClass}
        disabled={currentPage <= 0}
        onClick={onPrevious}
        type="button"
      >
        Previous
      </button>
      <span>
        {label} {currentPage + 1} / {totalPages}
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

const headerCellClass =
  'whitespace-nowrap px-4 py-3 text-xs font-extrabold uppercase'
const bodyCellClass = 'whitespace-nowrap px-4 py-3 text-[#9db2d0]'
const paginationButtonClass =
  'min-h-8 rounded-md border border-[#21304a] bg-[#0f1727] px-3 text-[#dce8ff] transition hover:border-[#334666] hover:bg-[#131e31] disabled:cursor-not-allowed disabled:opacity-50'
