import { Check, Pencil, X } from 'lucide-react'
import { useState } from 'react'
import {
  type PositionResponse,
  type UpdatePositionExitPricesPayload,
} from '../api/positionApi'
import { formatOptionalPrice } from '../../../utils/formatters'

type ExitPriceEditor = {
  positionId: string
  takeProfitPrice: string
  stopLossPrice: string
}

type PositionsTableProps = {
  positions: PositionResponse[]
  openPage: number
  closedPage: number
  openTotalPages: number
  closedTotalPages: number
  openTotalPositions: number
  closedTotalPositions: number
  isLoading: boolean
  error: string
  onOpenPageChange: (page: number | ((currentPage: number) => number)) => void
  onClosedPageChange: (page: number | ((currentPage: number) => number)) => void
  onClosePosition: (positionId: string) => Promise<unknown>
  onUpdateExitPrices: (
    positionId: string,
    payload: UpdatePositionExitPricesPayload,
  ) => Promise<unknown>
}

export function PositionsTable({
  closedPage,
  closedTotalPages,
  closedTotalPositions,
  error,
  isLoading,
  onClosePosition,
  onClosedPageChange,
  onOpenPageChange,
  onUpdateExitPrices,
  openPage,
  openTotalPages,
  openTotalPositions,
  positions,
}: PositionsTableProps) {
  const [closingPositionId, setClosingPositionId] = useState('')
  const [savingExitPositionId, setSavingExitPositionId] = useState('')
  const [exitPriceEditor, setExitPriceEditor] =
    useState<ExitPriceEditor | null>(null)
  const [actionError, setActionError] = useState('')

  async function handleClosePosition(positionId: string) {
    setClosingPositionId(positionId)
    setActionError('')

    try {
      await onClosePosition(positionId)
    } catch (caughtError) {
      setActionError(
        caughtError instanceof Error
          ? caughtError.message
          : 'Failed to close position',
      )
    } finally {
      setClosingPositionId('')
    }
  }

  function startEditingExitPrices(position: PositionResponse) {
    setExitPriceEditor({
      positionId: position.id,
      takeProfitPrice: editablePriceValue(position.takeProfitPrice),
      stopLossPrice: editablePriceValue(position.stopLossPrice),
    })
  }

  async function handleSaveExitPrices(positionId: string) {
    if (!exitPriceEditor || exitPriceEditor.positionId !== positionId) {
      return
    }

    if (
      !isOptionalPositivePrice(exitPriceEditor.takeProfitPrice) ||
      !isOptionalPositivePrice(exitPriceEditor.stopLossPrice)
    ) {
      setActionError('TP and SL must be empty or greater than zero')
      return
    }

    setSavingExitPositionId(positionId)
    setActionError('')

    try {
      await onUpdateExitPrices(positionId, {
        takeProfitPrice: normalizeOptionalPrice(exitPriceEditor.takeProfitPrice),
        stopLossPrice: normalizeOptionalPrice(exitPriceEditor.stopLossPrice),
      })
      setExitPriceEditor(null)
    } catch (caughtError) {
      setActionError(
        caughtError instanceof Error
          ? caughtError.message
          : 'Failed to update exit prices',
      )
    } finally {
      setSavingExitPositionId('')
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

      <div className="watchlist-scroll max-w-full overflow-x-auto rounded-md border border-[#1e293b]">
        <table className="w-full min-w-240 border-collapse text-left text-[13px]">
          <thead className="bg-[#0f1727] text-[#9db2d0]">
            <tr>
              <th className={headerCellClass}>Symbol</th>
              <th className={headerCellClass}>Side</th>
              <th className={headerCellClass}>Status</th>
              <th className={headerCellClass}>Qty</th>
              <th className={headerCellClass}>Value</th>
              <th className={headerCellClass}>Margin</th>
              <th className={headerCellClass}>Entry price</th>
              <th className={headerCellClass}>Mark / Exit</th>
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
              const isEditingExitPrices =
                exitPriceEditor?.positionId === position.id
              const isSavingExitPrices = savingExitPositionId === position.id

              return (
                <tr className="border-t border-[#1e293b]" key={position.id}>
                  <td className="px-3 py-2.5 font-bold text-[#f7fbff]">
                    {position.symbol}
                  </td>
                  <td className="px-3 py-2.5">
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
                  <td className="px-3 py-2.5">
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
                    {formatQuantity(position.quantity)}
                  </td>
                  <td className={bodyCellClass}>
                    {formatMoney(calculatePositionValue(position))}
                  </td>
                  <td className={bodyCellClass}>
                    {formatMoney(position.marginUsed)}
                  </td>
                  <td className={bodyCellClass}>
                    {formatOptionalPrice(position.avgEntryPrice)}
                  </td>
                  <td className={bodyCellClass}>
                    {formatOptionalPrice(position.currentPrice)}
                  </td>
                  <td className={bodyCellClass}>
                    {isEditingExitPrices ? (
                      <input
                        className={exitPriceInputClass}
                        inputMode="decimal"
                        onChange={(event) =>
                          setExitPriceEditor((currentEditor) =>
                            currentEditor
                              ? {
                                  ...currentEditor,
                                  takeProfitPrice: event.target.value,
                                }
                              : currentEditor,
                          )
                        }
                        placeholder="--"
                        type="text"
                        value={exitPriceEditor?.takeProfitPrice ?? ''}
                      />
                    ) : (
                      formatOptionalPrice(position.takeProfitPrice)
                    )}
                  </td>
                  <td className={bodyCellClass}>
                    {isEditingExitPrices ? (
                      <input
                        className={exitPriceInputClass}
                        inputMode="decimal"
                        onChange={(event) =>
                          setExitPriceEditor((currentEditor) =>
                            currentEditor
                              ? {
                                  ...currentEditor,
                                  stopLossPrice: event.target.value,
                                }
                              : currentEditor,
                          )
                        }
                        placeholder="--"
                        type="text"
                        value={exitPriceEditor?.stopLossPrice ?? ''}
                      />
                    ) : (
                      formatOptionalPrice(position.stopLossPrice)
                    )}
                  </td>
                  <td
                    className={[
                      'whitespace-nowrap px-3 py-2.5 font-bold',
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
                  <td className="whitespace-nowrap px-3 py-2.5">
                    {position.status === 'OPEN' ? (
                      <div className="flex gap-1.5">
                        {isEditingExitPrices ? (
                          <>
                            <button
                              aria-label="Save exit prices"
                              className={actionButtonClass}
                              disabled={isSavingExitPrices}
                              onClick={() => void handleSaveExitPrices(position.id)}
                              title="Save"
                              type="button"
                            >
                              {isSavingExitPrices ? (
                                '...'
                              ) : (
                                <Check aria-hidden="true" size={14} />
                              )}
                            </button>
                            <button
                              aria-label="Cancel editing exit prices"
                              className={secondaryButtonClass}
                              disabled={isSavingExitPrices}
                              onClick={() => setExitPriceEditor(null)}
                              title="Cancel"
                              type="button"
                            >
                              <X aria-hidden="true" size={14} />
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              aria-label="Edit exit prices"
                              className={secondaryButtonClass}
                              onClick={() => startEditingExitPrices(position)}
                              title="Edit TP/SL"
                              type="button"
                            >
                              <Pencil aria-hidden="true" size={13} />
                            </button>
                            <button
                              aria-label="Close position"
                              className={dangerButtonClass}
                              disabled={isClosing}
                              onClick={() => void handleClosePosition(position.id)}
                              title="Close"
                              type="button"
                            >
                              {isClosing ? (
                                '...'
                              ) : (
                                <X aria-hidden="true" size={14} />
                              )}
                            </button>
                          </>
                        )}
                      </div>
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
                  colSpan={13}
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

        {(error || actionError) && (
          <div className="border-t border-[#1e293b] bg-[#ff5367]/12 px-4 py-3 text-sm font-bold text-[#ffdce1]">
            {actionError || error}
          </div>
        )}
      </div>

      <div className="mt-3 grid gap-2 md:grid-cols-2">
        <PaginationControls
          currentPage={openPage}
          label="Open"
          onNext={() =>
            onOpenPageChange((currentPage) =>
              Math.min(currentPage + 1, Math.max(openTotalPages - 1, 0)),
            )
          }
          onPrevious={() =>
            onOpenPageChange((currentPage) => Math.max(currentPage - 1, 0))
          }
          totalPages={openTotalPages}
        />
        <PaginationControls
          currentPage={closedPage}
          label="Closed"
          onNext={() =>
            onClosedPageChange((currentPage) =>
              Math.min(currentPage + 1, Math.max(closedTotalPages - 1, 0)),
            )
          }
          onPrevious={() =>
            onClosedPageChange((currentPage) => Math.max(currentPage - 1, 0))
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

function calculatePositionValue(position: PositionResponse) {
  const quantity = Number(position.quantity)
  const price = Number(position.currentPrice)

  if (!Number.isFinite(quantity) || !Number.isFinite(price)) {
    return null
  }

  return quantity * price
}

function editablePriceValue(value: string | number | null) {
  if (value === null) {
    return ''
  }

  return String(value)
}

function isOptionalPositivePrice(value: string) {
  if (!value.trim()) {
    return true
  }

  return Number(value) > 0
}

function normalizeOptionalPrice(value: string) {
  const trimmedValue = value.trim()
  return trimmedValue ? trimmedValue : null
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
  'whitespace-nowrap px-3 py-2.5 text-xs font-extrabold uppercase'
const bodyCellClass = 'whitespace-nowrap px-3 py-2.5 text-[#9db2d0]'
const exitPriceInputClass =
  'h-7 w-24 rounded border border-[#21304a] bg-[#0d1627] px-2 text-xs font-semibold text-[#f7fbff] outline-none placeholder:text-[#6f829f] focus:border-[#6f84ff]'
const actionButtonClass =
  'grid size-7 place-items-center rounded-md border border-[#00d084]/30 bg-[#00d084]/12 text-sm font-bold text-[#b9ffe9] transition hover:bg-[#00d084]/20 disabled:cursor-not-allowed disabled:opacity-60'
const secondaryButtonClass =
  'grid size-7 place-items-center rounded-md border border-[#21304a] bg-[#0f1727] text-sm font-bold text-[#dce8ff] transition hover:border-[#334666] hover:bg-[#131e31] disabled:cursor-not-allowed disabled:opacity-60'
const dangerButtonClass =
  'grid size-7 place-items-center rounded-md border border-[#ff5367]/30 bg-[#ff5367]/12 text-sm font-bold text-[#ffdce1] transition hover:bg-[#ff5367]/20 disabled:cursor-not-allowed disabled:opacity-60'
const paginationButtonClass =
  'min-h-8 rounded-md border border-[#21304a] bg-[#0f1727] px-3 text-[#dce8ff] transition hover:border-[#334666] hover:bg-[#131e31] disabled:cursor-not-allowed disabled:opacity-50'
