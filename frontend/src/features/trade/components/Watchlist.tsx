const symbols = ['AAPL', 'TSLA', 'NVDA', 'AMZN', 'MSFT']

export function Watchlist() {
  return (
    <section className="rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.22)]">
      <h2 className="mb-4 text-xl font-bold">Watchlist</h2>

      <div className="grid gap-3">
        {symbols.map((symbol) => (
          <div
            className="flex items-center justify-between rounded-md border border-[#1e293b] bg-[#0f1727] px-4 py-3"
            key={symbol}
          >
            <div>
              <p className="mb-0.5 font-semibold">{symbol}</p>
              <p className="text-sm text-[#9db2d0]">Placeholder quote</p>
            </div>
            <span className="text-sm font-semibold text-[#00d084]">+0.00%</span>
          </div>
        ))}
      </div>
    </section>
  )
}
