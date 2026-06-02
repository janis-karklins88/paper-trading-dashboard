import {
  ColorType,
  LineSeries,
  TickMarkType,
  createChart,
  type IChartApi,
  type ISeriesApi,
  type LineData,
  type Time,
  type UTCTimestamp,
} from 'lightweight-charts'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  getAccountEquityCurve,
  type AccountEquitySnapshotResponse,
  type AccountEquityTimeframe,
} from '../trade/api/accountApi'
import { formatMoney } from '../../utils/formatters'

const EQUITY_CURVE_REFRESH_MS = 60_000
const CHART_TIME_ZONE = 'Europe/Riga'

const timeframes: Array<{ label: string; value: AccountEquityTimeframe }> = [
  { label: '1D', value: '1D' },
  { label: '1W', value: '1W' },
  { label: '1M', value: '1M' },
  { label: '1Y', value: '1Y' },
  { label: 'All', value: 'ALL' },
]

export function EquityCurveChart() {
  const chartContainerRef = useRef<HTMLDivElement | null>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const seriesRef = useRef<ISeriesApi<'Line'> | null>(null)

  const [timeframe, setTimeframe] = useState<AccountEquityTimeframe>('1D')
  const [snapshots, setSnapshots] = useState<AccountEquitySnapshotResponse[]>([])
  const [loadedTimeframe, setLoadedTimeframe] =
    useState<AccountEquityTimeframe | null>(null)
  const [error, setError] = useState('')

  const isLoading = loadedTimeframe !== timeframe

  const chartData = useMemo(
    () =>
      snapshots
        .map(toChartPoint)
        .filter(isValidChartPoint)
        .sort((left, right) => Number(left.time) - Number(right.time)),
    [snapshots],
  )

  useEffect(() => {
    const container = chartContainerRef.current

    if (!container) {
      return
    }

    const chart = createChart(container, {
      width: container.clientWidth,
      height: 320,
      layout: {
        background: { type: ColorType.Solid, color: '#0b1322' },
        textColor: '#9db2d0',
      },
      grid: {
        vertLines: { color: '#1e293b' },
        horzLines: { color: '#1e293b' },
      },
      rightPriceScale: {
        borderColor: '#21304a',
      },
      timeScale: {
        borderColor: '#21304a',
        timeVisible: true,
        secondsVisible: false,
        tickMarkFormatter: (time: Time, tickMarkType: TickMarkType) =>
          formatAxisTime(time, tickMarkType),
      },
      localization: {
        timeFormatter: (time: Time) => formatTooltipTime(time),
        priceFormatter: (price: number) => formatMoney(price),
      },
      crosshair: {
        horzLine: { color: '#7592ff' },
        vertLine: { color: '#7592ff' },
      },
    })

    const series = chart.addSeries(LineSeries, {
      color: '#7fa0ff',
      lineWidth: 2,
      priceFormat: {
        type: 'custom',
        formatter: (price: number) => formatMoney(price),
        minMove: 0.01,
      },
    })

    chartRef.current = chart
    seriesRef.current = series

    const resizeObserver = new ResizeObserver(([entry]) => {
      chart.resize(
        Math.floor(entry.contentRect.width),
        Math.floor(entry.contentRect.height) || 320,
      )
    })

    resizeObserver.observe(container)

    return () => {
      resizeObserver.disconnect()
      chart.remove()
      chartRef.current = null
      seriesRef.current = null
    }
  }, [])

  useEffect(() => {
    let isMounted = true

    loadEquityCurve(timeframe)
      .then((nextSnapshots) => {
        if (isMounted) {
          setSnapshots(nextSnapshots)
          setLoadedTimeframe(timeframe)
          setError('')
        }
      })
      .catch(() => {
        if (isMounted) {
          setSnapshots([])
          setLoadedTimeframe(timeframe)
          setError('Failed to load equity curve')
        }
      })

    return () => {
      isMounted = false
    }
  }, [timeframe])

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      loadEquityCurve(timeframe)
        .then((nextSnapshots) => {
          setSnapshots(nextSnapshots)
          setLoadedTimeframe(timeframe)
          setError('')
        })
        .catch(() => {
          setError('Failed to refresh equity curve')
        })
    }, EQUITY_CURVE_REFRESH_MS)

    return () => window.clearInterval(intervalId)
  }, [timeframe])

  useEffect(() => {
    const series = seriesRef.current

    if (!series) {
      return
    }

    series.setData(chartData)
    chartRef.current?.timeScale().fitContent()
  }, [chartData])

  return (
    <section className="flex min-h-105 flex-col rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.22)]">
      <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="text-base font-bold text-[#f7fbff]">Equity curve</h2>
          <p className="mt-1 text-sm text-[#9db2d0]">
            Account equity from persisted portfolio snapshots
          </p>
        </div>

        <div className="flex w-fit rounded-md border border-[#21304a] bg-[#0f1727] p-1">
          {timeframes.map((item) => (
            <button
              className={[
                'min-h-8 rounded px-3 text-sm font-bold transition',
                timeframe === item.value
                  ? 'bg-[#18234d] text-[#8fa2ff]'
                  : 'text-[#9db2d0] hover:text-white',
              ].join(' ')}
              key={item.value}
              onClick={() => setTimeframe(item.value)}
              type="button"
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      <div className="relative min-h-0 flex-1 overflow-hidden rounded-md border border-[#1e293b] bg-[#0b1322]">
        <div ref={chartContainerRef} className="h-full min-h-80 w-full" />

        {isLoading && (
          <div className="absolute inset-0 grid place-items-center bg-[#0b1322]/70 text-sm font-semibold text-[#9db2d0]">
            Loading equity curve
          </div>
        )}

        {!isLoading && !error && chartData.length === 0 && (
          <div className="absolute inset-0 grid place-items-center bg-[#0b1322]/70 px-6 text-center text-sm font-semibold text-[#9db2d0]">
            No equity snapshots yet
          </div>
        )}

        {error && (
          <div className="absolute inset-0 grid place-items-center bg-[#0b1322]/80 px-6 text-center text-sm font-semibold text-[#ffdce1]">
            {error}
          </div>
        )}
      </div>
    </section>
  )
}

async function loadEquityCurve(timeframe: AccountEquityTimeframe) {
  return getAccountEquityCurve(timeframe)
}

function toChartPoint(
  snapshot: AccountEquitySnapshotResponse,
): LineData<UTCTimestamp> {
  return {
    time: Math.floor(new Date(snapshot.timestamp).getTime() / 1000) as UTCTimestamp,
    value: Number(snapshot.equity),
  }
}

function isValidChartPoint(point: LineData<UTCTimestamp>) {
  return Number.isFinite(Number(point.time)) && Number.isFinite(point.value)
}

function formatAxisTime(time: Time, tickMarkType: TickMarkType) {
  const date = toDate(time)

  if (!date) {
    return null
  }

  if (
    tickMarkType === TickMarkType.Time ||
    tickMarkType === TickMarkType.TimeWithSeconds
  ) {
    return new Intl.DateTimeFormat('en-GB', {
      timeZone: CHART_TIME_ZONE,
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(date)
  }

  if (tickMarkType === TickMarkType.DayOfMonth) {
    return new Intl.DateTimeFormat('en-GB', {
      timeZone: CHART_TIME_ZONE,
      day: '2-digit',
      month: 'short',
    }).format(date)
  }

  if (tickMarkType === TickMarkType.Month) {
    return new Intl.DateTimeFormat('en-GB', {
      timeZone: CHART_TIME_ZONE,
      month: 'short',
      year: '2-digit',
    }).format(date)
  }

  return new Intl.DateTimeFormat('en-GB', {
    timeZone: CHART_TIME_ZONE,
    year: 'numeric',
  }).format(date)
}

function formatTooltipTime(time: Time) {
  const date = toDate(time)

  if (!date) {
    return ''
  }

  return new Intl.DateTimeFormat('en-GB', {
    timeZone: CHART_TIME_ZONE,
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

function toDate(time: Time) {
  if (typeof time === 'number') {
    return new Date(time * 1000)
  }

  if (typeof time === 'string') {
    return new Date(time)
  }

  return new Date(Date.UTC(time.year, time.month - 1, time.day))
}
