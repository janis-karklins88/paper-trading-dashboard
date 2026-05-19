import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import {
  getLatestPrice,
  type DefaultMarketSymbol,
  type MarketPrice,
} from '../api/marketDataApi'
import { placeOrder, type OrderResponse } from '../api/orderApi'
import {
  getTradingAccount,
  type TradingAccountResponse,
} from '../api/accountApi'
import { formatOptionalPrice } from '../../../utils/formatters'

type TradeTicketProps = {
  selectedAsset?: DefaultMarketSymbol
  selectedSymbol: string
}

type OrderSide = 'buy' | 'sell'
type OrderType = 'market' | 'limit'

const TEMP_PRICE_REFRESH_MS = 15_000
const TEMP_ACCOUNT_REFRESH_MS = 15_000

export function TradeTicket({
  selectedAsset,
  selectedSymbol,
}: TradeTicketProps) {
  const [side, setSide] = useState<OrderSide>('buy')
  const [orderType, setOrderType] = useState<OrderType>('market')
  const [marginAmount, setMarginAmount] = useState('')
  const [leverage, setLeverage] = useState('1')
  const [limitPrice, setLimitPrice] = useState('')
  const [latestPrice, setLatestPrice] = useState<MarketPrice | null>(null)
  const [tradingAccount, setTradingAccount] =
    useState<TradingAccountResponse | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [lastOrder, setLastOrder] = useState<OrderResponse | null>(null)

  const notionalValue = Number(marginAmount || 0) * Number(leverage || 1)
  const estimatedFee =
    notionalValue * (orderType === 'market' ? 0.0005 : 0.0001)
  const canSubmit =
    Boolean(selectedSymbol) &&
    Number(marginAmount) > 0 &&
    Number(leverage) >= 1 &&
    (orderType === 'market' || Number(limitPrice) > 0) &&
    !isSubmitting

  useEffect(() => {
    if (!selectedSymbol) {
      return
    }

    let isMounted = true

    const loadLatestPrice = () => {
      getLatestPrice(selectedSymbol)
        .then((price) => {
          if (isMounted) {
            setLatestPrice(price)
          }
        })
        .catch(() => {
          if (isMounted) {
            setLatestPrice(null)
          }
        })
    }

    loadLatestPrice()

    // Temporary polling until market-data websocket streaming is added.
    const intervalId = window.setInterval(loadLatestPrice, TEMP_PRICE_REFRESH_MS)

    return () => {
      isMounted = false
      window.clearInterval(intervalId)
    }
  }, [selectedSymbol])

  useEffect(() => {
    let isMounted = true

    const loadTradingAccount = () => {
      getTradingAccount()
        .then((account) => {
          if (isMounted) {
            setTradingAccount(account)
          }
        })
        .catch(() => {
          if (isMounted) {
            setTradingAccount(null)
          }
        })
    }

    loadTradingAccount()

    // Temporary polling until account websocket updates or shared trade state exist.
    const intervalId = window.setInterval(
      loadTradingAccount,
      TEMP_ACCOUNT_REFRESH_MS,
    )

    return () => {
      isMounted = false
      window.clearInterval(intervalId)
    }
  }, [])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setLastOrder(null)

    if (!canSubmit) {
      setError('Enter valid order details')
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
        takeProfitPrice: null,
        stopLossPrice: null,
      })

      setLastOrder(order)
      setTradingAccount(await getTradingAccount())
    } catch (caughtError) {
      setError(
        caughtError instanceof Error
          ? caughtError.message
          : 'Failed to place order',
      )
    } finally {
      setIsSubmitting(false)
    }
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
              onClick={() => setSide('buy')}
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
              onClick={() => setSide('sell')}
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
              onClick={() => setOrderType('market')}
              type="button"
            >
              Market
            </button>
            <button
              className={orderTypeButtonClass(orderType === 'limit')}
              onClick={() => setOrderType('limit')}
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
              className={fieldInputClass}
              inputMode="decimal"
              onChange={(event) => setLimitPrice(event.target.value)}
              placeholder="0.00"
              type="text"
              value={limitPrice}
            />
          </label>
        )}

        <label className={fieldLabelClass}>
          Position size
          <div className="relative">
            <span className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-sm font-semibold text-[#6f829f]">
              $
            </span>
            <input
              className={`${fieldInputClass} pl-7`}
              inputMode="decimal"
              onChange={(event) => setMarginAmount(event.target.value)}
              placeholder="1000.00"
              type="text"
              value={marginAmount}
            />
          </div>
        </label>

        <label className={fieldLabelClass}>
          Leverage
          <select
            className={fieldInputClass}
            onChange={(event) => setLeverage(event.target.value)}
            value={leverage}
          >
            <option value="1">1x</option>
            <option value="2">2x</option>
            <option value="3">3x</option>
            <option value="5">5x</option>
          </select>
        </label>

        <div className="grid grid-cols-2 gap-3">
          <label className={fieldLabelClass}>
            Take profit
            <input
              className={fieldInputClass}
              disabled
              inputMode="decimal"
              placeholder="Later"
              type="text"
            />
          </label>

          <label className={fieldLabelClass}>
            Stop loss
            <input
              className={fieldInputClass}
              disabled
              inputMode="decimal"
              placeholder="Later"
              type="text"
            />
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

const fieldLabelClass = 'grid gap-2 text-sm font-semibold text-[#dce8ff]'
const fieldInputClass =
  'min-h-11 w-full rounded-md border border-[#21304a] bg-[#0d1627] px-3 text-[#f7fbff] outline-none placeholder:text-[#6f829f] focus:border-[#6f84ff] disabled:cursor-not-allowed disabled:opacity-50'

function orderTypeButtonClass(isActive: boolean) {
  return [
    'min-h-9 rounded text-sm font-bold transition',
    isActive ? 'bg-[#18234d] text-[#7592ff]' : 'text-[#9db2d0] hover:text-white',
  ].join(' ')
}
