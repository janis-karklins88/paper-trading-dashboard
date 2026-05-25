import { useState } from 'react'
import type { FormEvent } from 'react'
import { type MarketPrice } from '../api/marketDataApi'
import { placeOrder, type OrderResponse } from '../api/orderApi'
import { type TradingAccountResponse } from '../api/accountApi'
import { formatOptionalPrice } from '../../../utils/formatters'
import type { SelectedAsset } from '../types'

type TradeTicketProps = {
  tradingAccount: TradingAccountResponse | null
  onOrderPlaced: (order: OrderResponse) => Promise<void> | void
  selectedAsset?: SelectedAsset
  selectedSymbol: string
  latestPrice: MarketPrice | null
}

type OrderSide = 'buy' | 'sell'
type OrderType = 'market' | 'limit'
type OrderFieldErrors = Partial<
  Record<
    | 'symbol'
    | 'limitPrice'
    | 'marginAmount'
    | 'leverage'
    | 'takeProfitPrice'
    | 'stopLossPrice',
    string
  >
>

export function TradeTicket({
  latestPrice,
  onOrderPlaced,
  selectedAsset,
  selectedSymbol,
  tradingAccount,
}: TradeTicketProps) {
  const [side, setSide] = useState<OrderSide>('buy')
  const [orderType, setOrderType] = useState<OrderType>('market')
  const [marginAmount, setMarginAmount] = useState('')
  const [leverage, setLeverage] = useState('1')
  const [limitPrice, setLimitPrice] = useState('')
  const [takeProfitPrice, setTakeProfitPrice] = useState('')
  const [stopLossPrice, setStopLossPrice] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<OrderFieldErrors>({})
  const [lastOrder, setLastOrder] = useState<OrderResponse | null>(null)

  const notionalValue = Number(marginAmount || 0) * Number(leverage || 1)
  const estimatedFee =
    notionalValue * (orderType === 'market' ? 0.0005 : 0.0001)
  const canSubmit =
    Boolean(selectedSymbol) &&
    !isSubmitting

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setFieldErrors({})
    setLastOrder(null)

    const validationErrors = validateOrderDetails({
      leverage,
      limitPrice,
      marginAmount,
      orderType,
      selectedSymbol,
      stopLossPrice,
      takeProfitPrice,
    })

    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors)
      return
    }

    setIsSubmitting(true)

    try {
      const order = await placeOrder({
        symbol: selectedSymbol,
        side: side === 'buy' ? 'BUY' : 'SELL',
        type: orderType === 'market' ? 'MARKET' : 'LIMIT',
        marginAmount,
        leverage,
        limitPrice: orderType === 'limit' ? limitPrice : null,
        takeProfitPrice: normalizeOptionalPrice(takeProfitPrice),
        stopLossPrice: normalizeOptionalPrice(stopLossPrice),
      })

      setLastOrder(order)
      await onOrderPlaced(order)
      resetTicket()
    } catch (caughtError) {
      const errorMessage =
        caughtError instanceof Error
          ? caughtError.message
          : 'Failed to place order'
      const mappedErrors = mapOrderErrorToFields(errorMessage)

      setFieldErrors(mappedErrors.fieldErrors)
      setError(mappedErrors.generalError)
    } finally {
      setIsSubmitting(false)
    }
  }

  function clearFieldError(field: keyof OrderFieldErrors) {
    setFieldErrors((currentErrors) => {
      if (!currentErrors[field]) {
        return currentErrors
      }

      const nextErrors = { ...currentErrors }
      delete nextErrors[field]
      return nextErrors
    })
  }

  function resetTicket() {
    setSide('buy')
    setOrderType('market')
    setMarginAmount('')
    setLeverage('1')
    setLimitPrice('')
    setTakeProfitPrice('')
    setStopLossPrice('')
    setFieldErrors({})
  }

  return (
    <section className="rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.22)]">
      <div className="mb-4">
        <h2 className="text-sm text-[#9db2d0]">Order entry</h2>
      </div>

      <form className="grid gap-4" onSubmit={handleSubmit}>
        <div className="rounded-md border border-[#21304a] bg-[#0d1627] px-3 py-2.5">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="mb-0.5 truncate text-sm font-bold text-[#f7fbff]">
                {selectedAsset?.symbol ?? (selectedSymbol || 'No symbol')}
              </p>
              <p className="truncate text-xs text-[#9db2d0]">
                {selectedAsset?.name ?? selectedSymbol}
              </p>
            </div>
            <div className="shrink-0 text-right">
              <p className="mb-0.5 text-sm font-bold text-[#f7fbff]">
                {formatOptionalPrice(latestPrice?.price)}
              </p>
              <p className="text-xs text-[#9db2d0]">Latest</p>
            </div>
          </div>
        </div>

        <div className="grid gap-2">
          <span className="text-sm font-semibold text-[#dce8ff]">Side</span>
          <div className="grid grid-cols-2 gap-3">
            <button
              className={[
                'min-h-10 rounded-md text-sm font-bold transition',
                side === 'buy'
                  ? 'bg-[#2fac7e] text-[#061a14]'
                  : 'bg-[#00d084]/15 text-[#00d084] hover:bg-[#00d084]/20',
              ].join(' ')}
              onClick={() => {
                setSide('buy')
                clearFieldError('takeProfitPrice')
                clearFieldError('stopLossPrice')
              }}
              type="button"
            >
              Buy
            </button>
            <button
              className={[
                'min-h-10 rounded-md text-sm font-bold transition',
                side === 'sell'
                  ? 'bg-[#a33d49] text-white'
                  : 'bg-[#ff5367]/15 text-[#ff5367] hover:bg-[#ff5367]/20',
              ].join(' ')}
              onClick={() => {
                setSide('sell')
                clearFieldError('takeProfitPrice')
                clearFieldError('stopLossPrice')
              }}
              type="button"
            >
              Sell
            </button>
          </div>
        </div>

        <div className="grid gap-2">
          <span className="text-sm font-semibold text-[#dce8ff]">
            Order type
          </span>
          <div className="grid grid-cols-2 rounded-md border border-[#21304a] bg-[#0f1727] p-1">
            <button
              className={orderTypeButtonClass(orderType === 'market')}
              onClick={() => {
                setOrderType('market')
                clearFieldError('limitPrice')
              }}
              type="button"
            >
              Market
            </button>
            <button
              className={orderTypeButtonClass(orderType === 'limit')}
              onClick={() => {
                setOrderType('limit')
                clearFieldError('limitPrice')
              }}
              type="button"
            >
              Limit
            </button>
          </div>
        </div>

        {orderType === 'limit' && (
          <label className={fieldLabelClass}>
            Limit price
            <input
              className={fieldInputClass(Boolean(fieldErrors.limitPrice))}
              inputMode="decimal"
              onChange={(event) => {
                setLimitPrice(event.target.value)
                clearFieldError('limitPrice')
              }}
              placeholder="0.00"
              type="text"
              value={limitPrice}
            />
            <FieldError message={fieldErrors.limitPrice} />
          </label>
        )}

        <label className={fieldLabelClass}>
          Position size
          <div className="relative">
            <span className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-sm font-semibold text-[#6f829f]">
              $
            </span>
            <input
              className={`${fieldInputClass(Boolean(fieldErrors.marginAmount))} pl-7`}
              inputMode="decimal"
              onChange={(event) => {
                setMarginAmount(event.target.value)
                clearFieldError('marginAmount')
              }}
              placeholder="1000.00"
              type="text"
              value={marginAmount}
            />
          </div>
          <FieldError message={fieldErrors.marginAmount} />
        </label>

        <label className={fieldLabelClass}>
          Leverage
          <select
            className={fieldInputClass(Boolean(fieldErrors.leverage))}
            onChange={(event) => {
              setLeverage(event.target.value)
              clearFieldError('leverage')
            }}
            value={leverage}
          >
            <option value="1">1x</option>
            <option value="2">2x</option>
            <option value="3">3x</option>
            <option value="5">5x</option>
          </select>
          <FieldError message={fieldErrors.leverage} />
        </label>

        <div className="grid grid-cols-2 gap-3">
          <label className={fieldLabelClass}>
            Take profit
            <input
              className={fieldInputClass(Boolean(fieldErrors.takeProfitPrice))}
              inputMode="decimal"
              onChange={(event) => {
                setTakeProfitPrice(event.target.value)
                clearFieldError('takeProfitPrice')
              }}
              placeholder="Optional"
              type="text"
              value={takeProfitPrice}
            />
            <FieldError message={fieldErrors.takeProfitPrice} />
          </label>

          <label className={fieldLabelClass}>
            Stop loss
            <input
              className={fieldInputClass(Boolean(fieldErrors.stopLossPrice))}
              inputMode="decimal"
              onChange={(event) => {
                setStopLossPrice(event.target.value)
                clearFieldError('stopLossPrice')
              }}
              placeholder="Optional"
              type="text"
              value={stopLossPrice}
            />
            <FieldError message={fieldErrors.stopLossPrice} />
          </label>
        </div>

        <div className="rounded-md border border-[#1e293b] bg-[#0f1727] p-3 text-sm">
          <div className="mb-2 flex justify-between gap-3 text-[#9db2d0]">
            <span>Available balance</span>
            <span className="font-semibold text-[#eef4ff]">
              {formatMoney(tradingAccount?.cashBalance)}
            </span>
          </div>
          <div className="mb-2 flex justify-between gap-3 text-[#9db2d0]">
            <span>Estimated margin</span>
            <span className="font-semibold text-[#eef4ff]">
              {formatMoney(marginAmount)}
            </span>
          </div>
          <div className="flex justify-between gap-3 text-[#9db2d0]">
            <span>Estimated fee</span>
            <span className="font-semibold text-[#eef4ff]">
              {formatMoney(estimatedFee)}
            </span>
          </div>
        </div>

        {error && (
          <p className="rounded-md bg-[#ff5367]/12 px-3 py-2.5 text-sm font-bold text-[#ffdce1]">
            {error}
          </p>
        )}

        {lastOrder && (
          <p className="rounded-md bg-[#00d084]/12 px-3 py-2.5 text-sm font-bold text-[#00d084]">
            {lastOrder.status} {lastOrder.side} order placed
          </p>
        )}

        <button
          className={[
            'min-h-11 rounded-md font-black transition',
            side === 'buy'
              ? 'bg-[#2fac7e] text-[#061a14] hover:bg-[#22ffb2]'
              : 'bg-[#ce4e5d] text-white hover:bg-[#a83845]',
            !canSubmit ? 'cursor-not-allowed opacity-60' : '',
          ].join(' ')} 
          disabled={!canSubmit}
          type="submit"
        >
          {isSubmitting
            ? 'Placing order'
            : `Place ${side === 'buy' ? 'buy' : 'sell'} order`}
        </button>
      </form>
    </section>
  )
}

function formatMoney(value?: number | string | null) {
  const numericValue = Number(value)

  if (!Number.isFinite(numericValue) || numericValue <= 0) {
    return '$0.00'
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 8,
  }).format(numericValue)
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

const fieldLabelClass = 'grid gap-2 text-sm font-semibold text-[#dce8ff]'

function fieldInputClass(hasError = false) {
  return [
    'min-h-11 w-full rounded-md border bg-[#0d1627] px-3 text-[#f7fbff] outline-none placeholder:text-[#6f829f] disabled:cursor-not-allowed disabled:opacity-50',
    hasError
      ? 'border-[#ff5367] focus:border-[#ff5367]'
      : 'border-[#21304a] focus:border-[#6f84ff]',
  ].join(' ')
}

function FieldError({ message }: { message?: string }) {
  if (!message) {
    return null
  }

  return <span className="text-xs font-bold text-[#ffdce1]">{message}</span>
}

function validateOrderDetails({
  leverage,
  limitPrice,
  marginAmount,
  orderType,
  selectedSymbol,
  stopLossPrice,
  takeProfitPrice,
}: {
  leverage: string
  limitPrice: string
  marginAmount: string
  orderType: OrderType
  selectedSymbol: string
  stopLossPrice: string
  takeProfitPrice: string
}) {
  const errors: OrderFieldErrors = {}

  if (!selectedSymbol) {
    errors.symbol = 'Select a symbol first'
  }

  if (!isPositiveNumber(marginAmount)) {
    errors.marginAmount = 'Position size must be greater than zero'
  }

  if (!isNumberAtLeast(leverage, 1)) {
    errors.leverage = 'Leverage must be at least 1x'
  }

  if (orderType === 'limit' && !isPositiveNumber(limitPrice)) {
    errors.limitPrice = 'Limit price must be greater than zero'
  }

  if (!isOptionalPositivePrice(takeProfitPrice)) {
    errors.takeProfitPrice = 'Take profit must be greater than zero'
  }

  if (!isOptionalPositivePrice(stopLossPrice)) {
    errors.stopLossPrice = 'Stop loss must be greater than zero'
  }

  return errors
}

function mapOrderErrorToFields(message: string) {
  const normalizedMessage = message.toLowerCase()
  const fieldErrors: OrderFieldErrors = {}

  for (const fieldMessage of message.split(';')) {
    const trimmedMessage = fieldMessage.trim()
    const [fieldName, ...details] = trimmedMessage.split(':')
    const fieldErrorMessage = details.join(':').trim() || trimmedMessage

    if (fieldName in fieldErrorMap) {
      fieldErrors[fieldErrorMap[fieldName]] = fieldErrorMessage
    }
  }

  if (normalizedMessage.includes('take profit')) {
    fieldErrors.takeProfitPrice = message
  }

  if (normalizedMessage.includes('stop loss')) {
    fieldErrors.stopLossPrice = message
  }

  if (
    normalizedMessage.includes('limit price') ||
    normalizedMessage.includes('limit order requires')
  ) {
    fieldErrors.limitPrice = message
  }

  if (
    normalizedMessage.includes('cash balance') ||
    normalizedMessage.includes('margin')
  ) {
    fieldErrors.marginAmount = message
  }

  if (normalizedMessage.includes('leverage')) {
    fieldErrors.leverage = message
  }

  return {
    fieldErrors,
    generalError: Object.keys(fieldErrors).length > 0 ? '' : message,
  }
}

const fieldErrorMap: Record<string, keyof OrderFieldErrors> = {
  leverage: 'leverage',
  limitPrice: 'limitPrice',
  marginAmount: 'marginAmount',
  stopLossPrice: 'stopLossPrice',
  symbol: 'symbol',
  takeProfitPrice: 'takeProfitPrice',
}

function isPositiveNumber(value: string) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0
}

function isNumberAtLeast(value: string, minimum: number) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue >= minimum
}

function orderTypeButtonClass(isActive: boolean) {
  return [
    'min-h-9 rounded text-sm font-bold transition',
    isActive ? 'bg-[#18234d] text-[#7592ff]' : 'text-[#9db2d0] hover:text-white',
  ].join(' ')
}
