import { useEffect, useMemo, useState } from 'react'
import { OrdersTable } from './components/OrdersTable'
import { PositionsTable } from './components/PositionsTable'
import { TradeTicket } from './components/TradeTicket'
import { TradingChart } from './components/TradingChart'
import { Watchlist } from './components/Watchlist'
import { trackActiveSymbol } from './api/marketDataApi'
import { useSelectedMarketPrice } from './hooks/useSelectedMarketPrice'
import { useTradeState } from './hooks/useTradeState'
import { buildChartLevels } from './utils/chartLevels'
import type { SelectedAsset } from './types'

const ACTIVE_SYMBOL_HEARTBEAT_MS = 15_000

export function TradeWorkspace() {
  const [selectedSymbol, setSelectedSymbol] = useState('')
  const selectedPrice = useSelectedMarketPrice(selectedSymbol)
  const tradeState = useTradeState()

  const selectedAsset = useMemo<SelectedAsset | undefined>(() => {
    if (!selectedSymbol) {
      return undefined
    }

    return {
      symbol: selectedSymbol,
      name: selectedSymbol,
      quoteSymbol: selectedSymbol,
    }
  }, [selectedSymbol])

  const chartLevels = useMemo(
    () =>
      buildChartLevels(
        selectedSymbol,
        tradeState.positions,
        tradeState.orders,
      ),
    [selectedSymbol, tradeState.orders, tradeState.positions],
  )

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
            chartLevels={chartLevels}
            selectedAsset={selectedAsset}
            selectedPrice={selectedPrice}
            selectedSymbol={selectedSymbol}
          />
        </div>
        <div className="min-w-0 xl:col-span-3">
          <TradeTicket
            latestPrice={selectedPrice}
            onOrderPlaced={tradeState.handleOrderPlaced}
            selectedAsset={selectedAsset}
            selectedSymbol={selectedSymbol}
            tradingAccount={tradeState.tradingAccount}
          />
        </div>
      </div>

      <div className="grid gap-4 xl:grid-cols-12">
        <div className="grid min-w-0 gap-4 xl:col-span-9">
          <PositionsTable
            closedPage={tradeState.closedPage}
            closedTotalPages={tradeState.closedTotalPages}
            closedTotalPositions={tradeState.closedTotalPositions}
            error={tradeState.positionsError}
            isLoading={tradeState.isLoadingPositions}
            onClosePosition={tradeState.closePosition}
            onClosedPageChange={tradeState.setClosedPage}
            onOpenPageChange={tradeState.setOpenPage}
            onUpdateExitPrices={tradeState.savePositionExitPrices}
            openPage={tradeState.openPage}
            openTotalPages={tradeState.openTotalPages}
            openTotalPositions={tradeState.openTotalPositions}
            positions={tradeState.positions}
          />
          <OrdersTable
            error={tradeState.ordersError}
            isLoading={tradeState.isLoadingOrders}
            onCancelOrder={tradeState.cancelOrder}
            onPageChange={tradeState.setOrderPage}
            orders={tradeState.orders}
            page={tradeState.orderPage}
            totalOrders={tradeState.totalOrders}
            totalPages={tradeState.orderTotalPages}
          />
        </div>
        <div className="min-w-0 xl:col-span-3">
          <Watchlist
            onSelectSymbol={setSelectedSymbol}
            selectedPrice={selectedPrice}
            selectedSymbol={selectedSymbol}
          />
        </div>
      </div>
    </section>
  )
}
