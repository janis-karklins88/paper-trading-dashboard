import type { ReactNode } from 'react'

type AuthPageProps = {
  title: string
  children: ReactNode
}

export function AuthPage({ title, children }: AuthPageProps) {
  return (
    <main className="grid min-h-screen place-items-center bg-[#0b1322] bg-[radial-gradient(circle_at_78%_8%,rgba(76,102,255,0.12),transparent_28%)] p-6 text-[#f7fbff]">
      <section
        className="w-full max-w-105 rounded-lg border border-[#21304a] bg-[#121b2d]/95 p-7 shadow-[0_24px_60px_rgba(3,8,20,0.34)]"
        aria-label={title}
      >
        <div className="mb-7.5 flex items-center gap-3.5">
          <div className="grid h-11 w-11 place-items-center rounded-full bg-linear-to-br from-[#314cff] to-[#0078ff] font-black text-white shadow-[0_14px_34px_rgba(49,76,255,0.28)]">
            PT
          </div>
          <div>
            <p className="mb-2 text-xs font-extrabold uppercase text-[#a9c7ff]">
              Paper Trading
            </p>
            <h1 className="text-[22px] leading-none font-bold">
              Trading Dashboard
            </h1>
          </div>
        </div>

        <div className="mb-5.5">
          <h2 className="mb-2 text-[28px] leading-none font-bold">{title}</h2>
        </div>

        {children}
      </section>
    </main>
  )
}
