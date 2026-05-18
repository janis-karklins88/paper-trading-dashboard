const orders = [
  { symbol: 'AAPL', side: 'Buy', quantity: '10', status: 'Pending' },
  { symbol: 'TSLA', side: 'Sell', quantity: '2', status: 'Filled' },
]

export function OrdersTable() {
  return (
    <section className="rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.22)]">
      <h2 className="mb-4 text-xl font-bold">Orders</h2>

      <div className="overflow-hidden rounded-md border border-[#1e293b]">
        <table className="w-full border-collapse text-left text-sm">
          <thead className="bg-[#0f1727] text-[#9db2d0]">
            <tr>
              <th className="px-4 py-3 font-semibold">Symbol</th>
              <th className="px-4 py-3 font-semibold">Side</th>
              <th className="px-4 py-3 font-semibold">Qty</th>
              <th className="px-4 py-3 font-semibold">Status</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr className="border-t border-[#1e293b]" key={order.symbol}>
                <td className="px-4 py-3 font-semibold">{order.symbol}</td>
                <td className="px-4 py-3 text-[#9db2d0]">{order.side}</td>
                <td className="px-4 py-3 text-[#9db2d0]">{order.quantity}</td>
                <td className="px-4 py-3 text-[#9db2d0]">{order.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
