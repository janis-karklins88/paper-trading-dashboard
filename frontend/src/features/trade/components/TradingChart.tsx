export function TradingChart() {
  return (
    <section className="min-h-100 rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.22)]">
      <div className="mb-5 flex items-start justify-between gap-4">
        <div>
          <h2 className="mb-1 text-xl font-bold">Trading chart</h2>
          <p className="text-sm text-[#9db2d0]">Chart placeholder</p>
        </div>
        <span className="rounded-md bg-[#0f1727] px-3 py-1.5 text-sm font-semibold text-[#a9c7ff]">
          AAPL
        </span>
      </div>

      <div className="relative min-h-72 overflow-hidden rounded-md border border-[#1e293b] bg-[#0b1322]">
        <div className="absolute inset-0 bg-[linear-gradient(#1e293b_1px,transparent_1px),linear-gradient(90deg,#1e293b_1px,transparent_1px)] bg-size-[100%_56px,72px_100%] opacity-60" />
        <div className="absolute right-6 bottom-10 left-6 h-32 border-b-2 border-[#4c66ff] opacity-90 [clip-path:polygon(0_72%,10%_58%,18%_64%,30%_28%,42%_42%,52%_22%,62%_48%,74%_36%,86%_62%,100%_44%,100%_50%,86%_68%,74%_42%,62%_54%,52%_28%,42%_48%,30%_34%,18%_70%,10%_64%,0_78%)]" />
      </div>
    </section>
  )
}
