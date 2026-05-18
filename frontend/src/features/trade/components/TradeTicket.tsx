export function TradeTicket() {
  return (
    <section className="rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.22)]">
      <h2 className="mb-5 text-xl font-bold">Trade ticket</h2>

      <div className="grid gap-4">
        <label className="grid gap-2 text-sm font-semibold text-[#dce8ff]">
          Symbol
          <input
            className="min-h-11 rounded-md border border-[#21304a] bg-[#0d1627] px-3 text-[#f7fbff] outline-none placeholder:text-[#6f829f]"
            placeholder="AAPL"
            readOnly
          />
        </label>

        <div className="grid grid-cols-2 gap-3">
          <button className="min-h-10 rounded-md bg-[#00d084]/15 text-sm font-bold text-[#00d084]">
            Buy
          </button>
          <button className="min-h-10 rounded-md bg-[#ff5367]/15 text-sm font-bold text-[#ff5367]">
            Sell
          </button>
        </div>

        <label className="grid gap-2 text-sm font-semibold text-[#dce8ff]">
          Quantity
          <input
            className="min-h-11 rounded-md border border-[#21304a] bg-[#0d1627] px-3 text-[#f7fbff] outline-none placeholder:text-[#6f829f]"
            placeholder="0"
            readOnly
          />
        </label>

        <button className="min-h-11 rounded-md border border-[#5a70ff] bg-[#4c66ff] font-black text-white">
          Preview order
        </button>
      </div>
    </section>
  )
}
