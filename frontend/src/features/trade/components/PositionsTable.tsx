const positions = [
  { symbol: 'AAPL', quantity: '25', averagePrice: '$184.20', pnl: '+2.14%' },
  { symbol: 'NVDA', quantity: '8', averagePrice: '$121.45', pnl: '-0.72%' },
]

export function PositionsTable() {
  return (
    <section className="rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.22)]">
      <h2 className="mb-4 text-xl font-bold">Positions</h2>

      <div className="overflow-hidden rounded-md border border-[#1e293b]">
        <table className="w-full border-collapse text-left text-sm">
          <thead className="bg-[#0f1727] text-[#9db2d0]">
            <tr>
              <th className="px-4 py-3 font-semibold">Symbol</th>
              <th className="px-4 py-3 font-semibold">Qty</th>
              <th className="px-4 py-3 font-semibold">Avg price</th>
              <th className="px-4 py-3 font-semibold">P/L</th>
            </tr>
          </thead>
          <tbody>
            {positions.map((position) => (
              <tr className="border-t border-[#1e293b]" key={position.symbol}>
                <td className="px-4 py-3 font-semibold">{position.symbol}</td>
                <td className="px-4 py-3 text-[#9db2d0]">{position.quantity}</td>
                <td className="px-4 py-3 text-[#9db2d0]">
                  {position.averagePrice}
                </td>
                <td
                  className={[
                    'px-4 py-3 font-semibold',
                    position.pnl.startsWith('+')
                      ? 'text-[#00d084]'
                      : 'text-[#ff5367]',
                  ].join(' ')}
                >
                  {position.pnl}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
