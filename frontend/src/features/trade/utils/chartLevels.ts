import type { OrderResponse } from '../api/orderApi'
import type { PositionResponse } from '../api/positionApi'
import type { ChartLevel } from '../types'

export function buildChartLevels(
  selectedSymbol: string,
  positions: PositionResponse[],
  orders: OrderResponse[],
) {
  if (!selectedSymbol) {
    return []
  }

  const levels: ChartLevel[] = []
  const normalizedSelectedSymbol = normalizeSymbol(selectedSymbol)

  positions
    .filter(
      (position) =>
        position.status === 'OPEN' &&
        normalizeSymbol(position.symbol) === normalizedSelectedSymbol,
    )
    .forEach((position) => {
      pushLevel(levels, {
        id: `position:${position.id}:entry`,
        label: `${position.side === 'LONG' ? 'Long' : 'Short'} entry`,
        price: position.avgEntryPrice,
        color: '#9bb0ff',
        lineStyle: 'solid',
      })
      pushLevel(levels, {
        id: `position:${position.id}:tp`,
        label: 'TP',
        price: position.takeProfitPrice,
        color: '#00d084',
        lineStyle: 'dashed',
      })
      pushLevel(levels, {
        id: `position:${position.id}:sl`,
        label: 'SL',
        price: position.stopLossPrice,
        color: '#ff5367',
        lineStyle: 'dashed',
      })
    })

  orders
    .filter(
      (order) =>
        order.type === 'LIMIT' &&
        (order.status === 'OPEN' || order.status === 'PENDING') &&
        normalizeSymbol(order.symbol) === normalizedSelectedSymbol,
    )
    .forEach((order) => {
      pushLevel(levels, {
        id: `order:${order.id}:limit`,
        label: `${order.side === 'BUY' ? 'Buy' : 'Sell'} limit`,
        price: order.limitPrice,
        color: '#f6c85f',
        lineStyle: 'dashed',
      })
    })

  return levels
}

function pushLevel(
  levels: ChartLevel[],
  level: Omit<ChartLevel, 'price'> & { price: string | number | null },
) {
  const price = Number(level.price)

  if (!Number.isFinite(price) || price <= 0) {
    return
  }

  levels.push({
    ...level,
    price,
  })
}

function normalizeSymbol(symbol: string) {
  return symbol.trim().toUpperCase()
}
