import { useEffect, useMemo, useState } from 'react'
import {
  getDefaultCryptoSymbols,
  type DefaultMarketSymbol,
} from './api/marketDataApi'
import { OrdersTable } from './components/OrdersTable'
import { PositionsTable } from './components/PositionsTable'
import { TradeTicket } from './components/TradeTicket'
import { TradingChart } from './components/TradingChart'
import { Watchlist } from './components/Watchlist'

export function TradeWorkspace() {
  const [symbols, setSymbols] = useState<DefaultMarketSymbol[]>([])
  const [selectedSymbol, setSelectedSymbol] = useState('')
  const [isLoadingSymbols, setIsLoadingSymbols] = useState(true)
  const [symbolError, setSymbolError] = useState('')
  const [ordersRefreshKey, setOrdersRefreshKey] = useState(0)

  const selectedAsset = useMemo(
    () => symbols.find((symbol) => symbol.quoteSymbol === selectedSymbol),
    [selectedSymbol, symbols],
  )

  useEffect(() => {
    let isMounted = true

    getDefaultCryptoSymbols()
      .then((defaultSymbols) => {
        if (!isMounted) {
          return
        }

        setSymbols(defaultSymbols)
        setSelectedSymbol(defaultSymbols[0]?.quoteSymbol ?? '')
      })
      .catch(() => {
        if (isMounted) {
          setSymbolError('Failed to load available symbols')
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoadingSymbols(false)
        }
      })

    return () => {
      isMounted = false
    }
  }, [])

  return (
    <section className="grid gap-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="mb-2 text-xs font-extrabold uppercase text-[#a9c7ff]">
            Order entry
          </p>
        </div>

        <label className="grid gap-2 text-sm font-semibold text-[#dce8ff]">
          <select
            className="min-h-10 min-w-60 rounded-md border border-[#21304a] bg-[#0f1727] px-3 text-sm font-semibold text-[#eef4ff] outline-none"
            disabled={isLoadingSymbols || symbols.length === 0}
            onChange={(event) => setSelectedSymbol(event.target.value)}
            value={selectedSymbol}
          >
            {symbols.map((symbol) => (
              <option key={symbol.quoteSymbol} value={symbol.quoteSymbol}>
                {symbol.symbol} - {symbol.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      {symbolError && (
        <p className="rounded-md bg-[#ff5367]/12 px-3 py-2.5 text-sm font-bold text-[#ffdce1]">
          {symbolError}
        </p>
      )}

      <div className="grid gap-4 xl:grid-cols-12">
        <div className="min-w-0 xl:col-span-9">
          <TradingChart
            selectedAsset={selectedAsset}
            selectedSymbol={selectedSymbol}
          />
        </div>
        <div className="min-w-0 xl:col-span-3">
          <TradeTicket
            selectedAsset={selectedAsset}
            selectedSymbol={selectedSymbol}
          />
        </div>
      </div>

      <div className="grid gap-4 xl:grid-cols-12">
        <div className="grid min-w-0 gap-4 xl:col-span-9">
          <PositionsTable
            onPositionClosed={() =>
              setOrdersRefreshKey((currentKey) => currentKey + 1)
            }
          />
          <OrdersTable refreshKey={ordersRefreshKey} />
        </div>
        <div className="min-w-0 xl:col-span-3">
          <Watchlist />
        </div>
      </div>
    </section>
  )
}
