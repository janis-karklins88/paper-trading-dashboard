import {
  CandlestickSeries,
  ColorType,
  TickMarkType,
  createChart,
  type CandlestickData,
  type IChartApi,
  type ISeriesApi,
  type Time,
  type UTCTimestamp,
} from 'lightweight-charts'
import { useEffect, useRef, useState } from 'react'
import {
  getCandles,
  type CandleTimeframe,
  type MarketCandle,
} from '../api/marketDataApi'
import { formatPrice } from '../../../utils/formatters'
import type { SelectedAsset } from '../types'

const timeframes: Array<{ label: string; value: CandleTimeframe }> = [
  { label: '1m', value: '1m' },
  { label: '5m', value: '5m' },
  { label: '15m', value: '15m' },
  { label: '1h', value: '1h' },
  { label: '1d', value: '1d' },
]

const TEMP_CHART_REFRESH_MS = 15_000
const CHART_TIME_ZONE = 'Europe/Riga'

type TradingChartProps = {
  selectedAsset?: SelectedAsset
  selectedSymbol: string
}

export function TradingChart({
  selectedAsset,
  selectedSymbol,
}: TradingChartProps) {
  const chartContainerRef = useRef<HTMLDivElement | null>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const seriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null)
  const previousCandleKeyRef = useRef('')
  const previousSelectedSymbolRef = useRef('')

  const [timeframe, setTimeframe] = useState<CandleTimeframe>('1h')
  const [candles, setCandles] = useState<MarketCandle[]>([])
  const [loadedCandleKey, setLoadedCandleKey] = useState('')
  const [error, setError] = useState('')

  const candleKey = selectedSymbol ? `${selectedSymbol}:${timeframe}` : ''
  const isLoadingCandles = Boolean(candleKey && loadedCandleKey !== candleKey)

  useEffect(() => {
    const container = chartContainerRef.current

    if (!container) {
      return
    }

    const chart = createChart(container, {
      width: container.clientWidth,
      height: 360,
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
        priceFormatter: (price: number) => formatPrice(price),
      },
      crosshair: {
        horzLine: { color: '#7592ff' },
        vertLine: { color: '#7592ff' },
      },
    })

    const series = chart.addSeries(CandlestickSeries, {
      upColor: '#00d084',
      downColor: '#ff5367',
      borderUpColor: '#00d084',
      borderDownColor: '#ff5367',
      wickUpColor: '#00d084',
      wickDownColor: '#ff5367',
      priceFormat: {
        type: 'custom',
        formatter: (price: number) => formatPrice(price),
        minMove: 0.00000001,
      },
    })

    chartRef.current = chart
    seriesRef.current = series

    const resizeChart = () => {
      chart.resize(container.clientWidth, container.clientHeight || 360)
    }

    requestAnimationFrame(resizeChart)

    const resizeObserver = new ResizeObserver(([entry]) => {
      chart.resize(
        Math.floor(entry.contentRect.width),
        Math.floor(entry.contentRect.height) || 360,
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
    if (!selectedSymbol) {
      return
    }

    let isMounted = true
    const requestKey = `${selectedSymbol}:${timeframe}`

    getCandles(selectedSymbol, timeframe)
      .then((nextCandles) => {
        if (isMounted) {
          setCandles(nextCandles)
          setLoadedCandleKey(requestKey)
          setError('')
        }
      })
      .catch(() => {
        if (isMounted) {
          setCandles([])
          setLoadedCandleKey(requestKey)
          setError('Failed to load candles')
        }
      })

    return () => {
      isMounted = false
    }
  }, [selectedSymbol, timeframe])

  useEffect(() => {
    if (!selectedSymbol) {
      return
    }

    // Temporary polling until market-data websocket streaming is added.
    const intervalId = window.setInterval(() => {
      const requestKey = `${selectedSymbol}:${timeframe}`

      getCandles(selectedSymbol, timeframe)
        .then((nextCandles) => {
          setCandles(nextCandles)
          setLoadedCandleKey(requestKey)
          setError('')
        })
        .catch(() => {
          setLoadedCandleKey(requestKey)
          setError('Failed to refresh candles')
        })
    }, TEMP_CHART_REFRESH_MS)

    return () => window.clearInterval(intervalId)
  }, [selectedSymbol, timeframe])

  useEffect(() => {
    if (!seriesRef.current) {
      return
    }

    const chartData = candles
      .map(toChartCandle)
      .filter((candle) => isValidChartCandle(candle))
      .sort((left, right) => Number(left.time) - Number(right.time))

    const chart = chartRef.current
    const previousVisibleRange = chart?.timeScale().getVisibleLogicalRange()

    seriesRef.current.setData(chartData)

    if (candleKey !== previousCandleKeyRef.current) {
      const symbolChanged = selectedSymbol !== previousSelectedSymbolRef.current
      const shouldFitContent = !previousCandleKeyRef.current || !symbolChanged

      if (shouldFitContent) {
        chart?.timeScale().fitContent()
      } else if (previousVisibleRange) {
        chart?.timeScale().setVisibleLogicalRange(previousVisibleRange)
      }

      previousCandleKeyRef.current = candleKey
      previousSelectedSymbolRef.current = selectedSymbol
    }
  }, [candleKey, candles, selectedSymbol])

  return (
    <section className="rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.22)] flex h-full flex-col">
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          
          <p className="text-sm text-[#9db2d0]">
            {selectedAsset
              ? `${selectedAsset.name} - ${selectedAsset.quoteSymbol}`
              : 'Select an asset'}
          </p>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row">
          <div className="flex rounded-md border border-[#21304a] bg-[#0f1727] p-1">
            {timeframes.map((item) => (
              <button
                className={[
                  'min-h-8 rounded px-3 text-sm font-semibold transition',
                  timeframe === item.value
                    ? 'bg-[#18234d] text-[#7592ff]'
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
      </div>

      

      <div className="relative min-h-0 flex-1 overflow-hidden rounded-md border border-[#1e293b] bg-[#0b1322]">
        <div ref={chartContainerRef} className="h-full w-full" />

        {isLoadingCandles && (
          <div className="absolute inset-0 grid place-items-center bg-[#0b1322]/70 text-sm font-semibold text-[#9db2d0]">
            Loading chart
          </div>
        )}

        {!isLoadingCandles && !error && selectedSymbol && candles.length === 0 && (
          <div className="absolute inset-0 grid place-items-center bg-[#0b1322]/70 text-sm font-semibold text-[#9db2d0]">
            No candle data available
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

function toChartCandle(candle: MarketCandle): CandlestickData<UTCTimestamp> {
  return {
    time: Math.floor(new Date(candle.timestamp).getTime() / 1000) as UTCTimestamp,
    open: Number(candle.open),
    high: Number(candle.high),
    low: Number(candle.low),
    close: Number(candle.close),
  }
}

function isValidChartCandle(candle: CandlestickData<UTCTimestamp>) {
  return (
    Number.isFinite(Number(candle.time)) &&
    Number.isFinite(candle.open) &&
    Number.isFinite(candle.high) &&
    Number.isFinite(candle.low) &&
    Number.isFinite(candle.close)
  )
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
