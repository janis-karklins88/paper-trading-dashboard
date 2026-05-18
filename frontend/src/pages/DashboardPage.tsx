export function DashboardPage() {
  return (
    <section className="grid gap-6">
      <div>
        <p className="mb-2 text-xs font-extrabold uppercase text-[#a9c7ff]">
          Overview
        </p>
        <h1 className="mb-0 text-[34px] leading-none font-bold">Dashboard</h1>
      </div>

      <div className="min-h-55 rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-7 shadow-[0_18px_50px_rgba(3,8,20,0.22)]">
        <h2 className="mb-2.5 text-[22px] font-bold">Dashboard placeholder</h2>
        <p className="leading-relaxed text-[#9db2d0]">
          Portfolio summary, positions, charts, and watchlist will go here.
        </p>
      </div>
    </section>
  )
}
