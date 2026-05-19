import { useEffect, useState } from 'react'
import {
  closePosition,
  getOpenPositions,
  type PositionResponse,
} from '../api/positionApi'
import { formatOptionalPrice } from '../../../utils/formatters'

const POSITION_REFRESH_MS = 15_000

export function PositionsTable() {
  const [positions, setPositions] = useState<PositionResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [closingPositionId, setClosingPositionId] = useState('')
  const [error, setError] = useState('')

  const loadPositions = () =>
    getOpenPositions()
      .then((nextPositions) => {
        setPositions(nextPositions)
        setError('')
      })
      .catch(() => {
        setError('Failed to load positions')
      })
      .finally(() => {
        setIsLoading(false)
      })

  useEffect(() => {
    let isMounted = true

    const loadMountedPositions = () => {
      getOpenPositions()
        .then((nextPositions) => {
          if (isMounted) {
            setPositions(nextPositions)
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
  }, [])

  async function handleClosePosition(positionId: string) {
    setClosingPositionId(positionId)
    setError('')

    try {
      await closePosition(positionId)
      await loadPositions()
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
          {positions.length} open
        </span>
      </div>

      <div className="max-w-full overflow-x-auto rounded-md border border-[#1e293b]">
        <table className="w-full min-w-250 border-collapse text-left text-sm">
          <thead className="bg-[#0f1727] text-[#9db2d0]">
            <tr>
              <th className={headerCellClass}>Symbol</th>
              <th className={headerCellClass}>Side</th>
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
              const pnl = Number(position.unrealizedPnl)
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
                    {formatPnlMoney(position.unrealizedPnl)}
                    <span className="ml-1 text-xs">
                      ({formatPercent(pnlPercent)})
                    </span>
                  </td>
                  <td className={bodyCellClass}>
                    {formatDateTime(position.openedAt)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <button
                      className="min-h-8 rounded-md border border-[#ff5367]/30 bg-[#ff5367]/12 px-3 text-xs font-bold text-[#ffdce1] transition hover:bg-[#ff5367]/20 disabled:cursor-not-allowed disabled:opacity-60"
                      disabled={isClosing}
                      onClick={() => void handleClosePosition(position.id)}
                      type="button"
                    >
                      {isClosing ? 'Closing' : 'Close'}
                    </button>
                  </td>
                </tr>
              )
            })}

            {!isLoading && positions.length === 0 && (
              <tr className="border-t border-[#1e293b]">
                <td className="px-4 py-8 text-center text-[#9db2d0]" colSpan={9}>
                  No open positions
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
    </section>
  )
}

function calculatePnlPercent(position: PositionResponse) {
  const pnl = Number(position.unrealizedPnl)
  const margin = Number(position.marginUsed)

  if (!Number.isFinite(pnl) || !Number.isFinite(margin) || margin <= 0) {
    return null
  }

  return (pnl / margin) * 100
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

const headerCellClass =
  'whitespace-nowrap px-4 py-3 text-xs font-extrabold uppercase'
const bodyCellClass = 'whitespace-nowrap px-4 py-3 text-[#9db2d0]'
