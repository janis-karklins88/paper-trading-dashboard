import { OrdersTable } from './components/OrdersTable'
import { PositionsTable } from './components/PositionsTable'
import { TradeTicket } from './components/TradeTicket'
import { TradingChart } from './components/TradingChart'
import { Watchlist } from './components/Watchlist'

export function TradeWorkspace() {
  return (
    <section className="grid gap-6">
      <div>
        <p className="mb-2 text-xs font-extrabold uppercase text-[#a9c7ff]">
          Order entry
        </p>
        <h1 className="mb-0 text-[34px] leading-none font-bold">Trade</h1>
      </div>

      <div className="grid gap-4 xl:grid-cols-12">
        <div className="xl:col-span-8">
          <TradingChart />
        </div>
        <div className="xl:col-span-4">
          <TradeTicket />
        </div>
      </div>

      <div className="grid gap-4 xl:grid-cols-12">
        <div className="grid gap-4 xl:col-span-8">
          <OrdersTable />
          <PositionsTable />
        </div>
        <div className="xl:col-span-4">
          <Watchlist />
        </div>
      </div>
    </section>
  )
}
