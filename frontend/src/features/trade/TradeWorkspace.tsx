import { useEffect, useMemo, useState } from 'react'
import { OrdersTable } from './components/OrdersTable'
import { PositionsTable } from './components/PositionsTable'
import { TradeTicket } from './components/TradeTicket'
import { TradingChart } from './components/TradingChart'
import { Watchlist } from './components/Watchlist'
import { trackActiveSymbol } from './api/marketDataApi'

const ACTIVE_SYMBOL_HEARTBEAT_MS = 15_000

export function TradeWorkspace() {
  const [selectedSymbol, setSelectedSymbol] = useState('')
  const [ordersRefreshKey, setOrdersRefreshKey] = useState(0)

  const selectedAsset = useMemo(() => {
    if (!selectedSymbol) {
      return undefined
    }

    return {
      rank: 0,
      symbol: selectedSymbol,
      name: selectedSymbol,
      quoteSymbol: selectedSymbol,
    }
  }, [selectedSymbol])

  useEffect(() => {
    if (!selectedSymbol) {
      return
    }

    const trackSelectedSymbol = () => {
      trackActiveSymbol(selectedSymbol).catch(() => {
        // Best effort; the ticket can still fall back to direct latest-price polling.
      })
    }

    trackSelectedSymbol()
    const intervalId = window.setInterval(
      trackSelectedSymbol,
      ACTIVE_SYMBOL_HEARTBEAT_MS,
    )

    return () => window.clearInterval(intervalId)
  }, [selectedSymbol])

  return (
    <section className="grid gap-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="mb-2 text-xs font-extrabold uppercase text-[#a9c7ff]">
            Order entry
          </p>
          <p className="text-sm font-semibold text-[#dce8ff]">
            {selectedSymbol || 'Select a symbol from watchlist'}
          </p>
        </div>
      </div>

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
          <Watchlist
            onSelectSymbol={setSelectedSymbol}
            selectedSymbol={selectedSymbol}
          />
        </div>
      </div>
    </section>
  )
}
