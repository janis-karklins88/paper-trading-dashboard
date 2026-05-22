import { useEffect, useMemo, useState } from 'react'
import { getCurrentUser } from '../../../auth/authApi'
import { getOrders, type OrderResponse } from '../api/orderApi'
import { subscribeToOrderUpdates } from '../api/orderStream'
import { getOpenPositions, type PositionResponse } from '../api/positionApi'
import { subscribeToPositionUpdates } from '../api/positionStream'
import type { ChartLevel } from '../types'

const CHART_LEVEL_REFRESH_MS = 60_000
const CHART_LEVEL_PAGE_SIZE = 100

export function useChartLevels(selectedSymbol: string) {
  const [positions, setPositions] = useState<PositionResponse[]>([])
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [userId, setUserId] = useState('')

  useEffect(() => {
    let isMounted = true

    getCurrentUser()
      .then((user) => {
        if (isMounted) {
          setUserId(user.id)
        }
      })
      .catch(() => {
        if (isMounted) {
          setUserId('')
        }
      })

    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    if (!selectedSymbol) {
      setPositions([])
      setOrders([])
      return
    }

    let isMounted = true

    const loadChartLevels = () => {
      Promise.all([
        getOpenPositions(0, CHART_LEVEL_PAGE_SIZE),
        getOrders(0, CHART_LEVEL_PAGE_SIZE),
      ])
        .then(([openPositions, orderPage]) => {
          if (!isMounted) {
            return
          }

          setPositions(openPositions.content)
          setOrders(orderPage.content)
        })
        .catch(() => {
          if (!isMounted) {
            return
          }

          setPositions([])
          setOrders([])
        })
    }

    loadChartLevels()
    const intervalId = window.setInterval(
      loadChartLevels,
      CHART_LEVEL_REFRESH_MS,
    )

    return () => {
      isMounted = false
      window.clearInterval(intervalId)
    }
  }, [selectedSymbol])

  useEffect(() => {
    if (!userId) {
      return
    }

    return subscribeToPositionUpdates(userId, (positionUpdate) => {
      setPositions((currentPositions) =>
        patchOpenPosition(currentPositions, positionUpdate),
      )
    })
  }, [userId])

  useEffect(() => {
    if (!userId) {
      return
    }

    return subscribeToOrderUpdates(userId, (orderUpdate) => {
      setOrders((currentOrders) => patchOrder(currentOrders, orderUpdate))
    })
  }, [userId])

  return useMemo(
    () => buildChartLevels(selectedSymbol, positions, orders),
    [selectedSymbol, positions, orders],
  )
}

function buildChartLevels(
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

function patchOpenPosition(
  positions: PositionResponse[],
  positionUpdate: PositionResponse,
) {
  const existingIndex = positions.findIndex(
    (position) => position.id === positionUpdate.id,
  )

  if (positionUpdate.status !== 'OPEN') {
    return positions.filter((position) => position.id !== positionUpdate.id)
  }

  if (existingIndex === -1) {
    return [positionUpdate, ...positions]
  }

  const nextPositions = [...positions]
  nextPositions[existingIndex] = positionUpdate
  return nextPositions
}

function patchOrder(orders: OrderResponse[], orderUpdate: OrderResponse) {
  const existingIndex = orders.findIndex((order) => order.id === orderUpdate.id)

  if (existingIndex === -1) {
    return [orderUpdate, ...orders]
  }

  const nextOrders = [...orders]
  nextOrders[existingIndex] = orderUpdate
  return nextOrders
}

function normalizeSymbol(symbol: string) {
  return symbol.trim().toUpperCase()
}
