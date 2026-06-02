import { useCallback, useEffect, useState } from 'react'
import { getCurrentUser } from '../../../auth/authApi'
import {
  getTradingAccount,
  type TradingAccountResponse,
} from '../api/accountApi'
import { subscribeToTradingAccountUpdates } from '../api/accountStream'
import {
  cancelOrder as cancelOrderRequest,
  getOrders,
  type OrderResponse,
} from '../api/orderApi'
import { subscribeToOrderUpdates } from '../api/orderStream'
import {
  closePosition as closePositionRequest,
  getClosedPositions,
  getOpenPositions,
  updatePositionExitPrices,
  type PositionResponse,
  type UpdatePositionExitPricesPayload,
} from '../api/positionApi'
import { subscribeToPositionUpdates } from '../api/positionStream'

const POSITIONS_REFRESH_MS = 60_000
const ORDERS_REFRESH_MS = 60_000
const ACCOUNT_REFRESH_MS = 15_000
export const POSITION_PAGE_SIZE = 10
export const ORDER_PAGE_SIZE = 10

export function useTradeState() {
  const [userId, setUserId] = useState('')
  const [positions, setPositions] = useState<PositionResponse[]>([])
  const [openPage, setOpenPage] = useState(0)
  const [closedPage, setClosedPage] = useState(0)
  const [openTotalPages, setOpenTotalPages] = useState(0)
  const [closedTotalPages, setClosedTotalPages] = useState(0)
  const [openTotalPositions, setOpenTotalPositions] = useState(0)
  const [closedTotalPositions, setClosedTotalPositions] = useState(0)
  const [isLoadingPositions, setIsLoadingPositions] = useState(true)
  const [positionsError, setPositionsError] = useState('')

  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [orderPage, setOrderPage] = useState(0)
  const [orderTotalPages, setOrderTotalPages] = useState(0)
  const [totalOrders, setTotalOrders] = useState(0)
  const [isLoadingOrders, setIsLoadingOrders] = useState(true)
  const [ordersError, setOrdersError] = useState('')

  const [tradingAccount, setTradingAccount] =
    useState<TradingAccountResponse | null>(null)

  const loadTradingAccount = useCallback(() => {
    return getTradingAccount()
      .then((account) => {
        setTradingAccount(account)
        return account
      })
      .catch(() => {
        setTradingAccount(null)
        return null
      })
  }, [])

  const loadPositionsForPages = useCallback(
    async (nextOpenPage: number, nextClosedPage: number) => {
      const [openPositions, closedPositions] = await Promise.all([
        getOpenPositions(nextOpenPage, POSITION_PAGE_SIZE),
        getClosedPositions(nextClosedPage, POSITION_PAGE_SIZE),
      ])

      setPositions([
        ...sortOpenPositions(openPositions.content),
        ...sortClosedPositions(closedPositions.content),
      ])
      setOpenTotalPages(openPositions.totalPages)
      setClosedTotalPages(closedPositions.totalPages)
      setOpenTotalPositions(openPositions.totalElements)
      setClosedTotalPositions(closedPositions.totalElements)
      setPositionsError('')
    },
    [],
  )

  const loadCurrentPositions = useCallback(() => {
    return loadPositionsForPages(openPage, closedPage)
      .catch(() => {
        setPositionsError('Failed to load positions')
      })
      .finally(() => {
        setIsLoadingPositions(false)
      })
  }, [closedPage, loadPositionsForPages, openPage])

  const loadOrders = useCallback(() => {
    return getOrders(orderPage, ORDER_PAGE_SIZE)
      .then((nextPage) => {
        setOrders(sortOrders(nextPage.content))
        setOrderTotalPages(nextPage.totalPages)
        setTotalOrders(nextPage.totalElements)
        setOrdersError('')
      })
      .catch(() => {
        setOrdersError('Failed to load orders')
      })
      .finally(() => {
        setIsLoadingOrders(false)
      })
  }, [orderPage])

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
    loadCurrentPositions()
    const intervalId = window.setInterval(
      loadCurrentPositions,
      POSITIONS_REFRESH_MS,
    )

    return () => window.clearInterval(intervalId)
  }, [loadCurrentPositions])

  useEffect(() => {
    loadOrders()
    const intervalId = window.setInterval(loadOrders, ORDERS_REFRESH_MS)

    return () => window.clearInterval(intervalId)
  }, [loadOrders])

  useEffect(() => {
    loadTradingAccount()
    const intervalId = window.setInterval(
      loadTradingAccount,
      ACCOUNT_REFRESH_MS,
    )

    return () => window.clearInterval(intervalId)
  }, [loadTradingAccount])

  useEffect(() => {
    if (!userId) {
      return
    }

    return subscribeToTradingAccountUpdates(userId, setTradingAccount)
  }, [userId])

  useEffect(() => {
    if (!userId) {
      return
    }

    return subscribeToPositionUpdates(userId, (positionUpdate) => {
      setPositions((currentPositions) =>
        patchPosition(currentPositions, positionUpdate),
      )
      loadTradingAccount()
    })
  }, [loadTradingAccount, userId])

  useEffect(() => {
    if (!userId) {
      return
    }

    return subscribeToOrderUpdates(userId, (orderUpdate) => {
      setOrders((currentOrders) =>
        patchOrder(currentOrders, orderUpdate, ORDER_PAGE_SIZE),
      )
      loadTradingAccount()
    })
  }, [loadTradingAccount, userId])

  const closePosition = useCallback(
    async (positionId: string) => {
      await closePositionRequest(positionId)
      setClosedPage(0)
      await Promise.all([
        loadPositionsForPages(openPage, 0),
        loadOrders(),
        loadTradingAccount(),
      ])
    },
    [loadOrders, loadPositionsForPages, loadTradingAccount, openPage],
  )

  const savePositionExitPrices = useCallback(
    async (positionId: string, payload: UpdatePositionExitPricesPayload) => {
      const updatedPosition = await updatePositionExitPrices(positionId, payload)
      setPositions((currentPositions) =>
        patchPosition(currentPositions, updatedPosition),
      )
      return updatedPosition
    },
    [],
  )

  const cancelOrder = useCallback(
    async (orderId: string) => {
      const updatedOrder = await cancelOrderRequest(orderId)
      setOrders((currentOrders) =>
        patchOrder(currentOrders, updatedOrder, ORDER_PAGE_SIZE),
      )
      await loadTradingAccount()
      return updatedOrder
    },
    [loadTradingAccount],
  )

  const handleOrderPlaced = useCallback(
    async (order: OrderResponse) => {
      setOrders((currentOrders) =>
        patchOrder(currentOrders, order, ORDER_PAGE_SIZE),
      )
      await Promise.all([
        loadCurrentPositions(),
        loadOrders(),
        loadTradingAccount(),
      ])
    },
    [loadCurrentPositions, loadOrders, loadTradingAccount],
  )

  return {
    positions,
    openPage,
    setOpenPage,
    closedPage,
    setClosedPage,
    openTotalPages,
    closedTotalPages,
    openTotalPositions,
    closedTotalPositions,
    isLoadingPositions,
    positionsError,
    closePosition,
    savePositionExitPrices,
    orders,
    orderPage,
    setOrderPage,
    orderTotalPages,
    totalOrders,
    isLoadingOrders,
    ordersError,
    cancelOrder,
    handleOrderPlaced,
    tradingAccount,
  }
}

export function patchPosition(
  positions: PositionResponse[],
  positionUpdate: PositionResponse,
) {
  const existingIndex = positions.findIndex(
    (position) => position.id === positionUpdate.id,
  )

  if (existingIndex === -1) {
    if (positionUpdate.status !== 'OPEN') {
      return positions
    }

    return [
      ...sortOpenPositions([
        positionUpdate,
        ...positions.filter((position) => position.status === 'OPEN'),
      ]),
      ...sortClosedPositions(
        positions.filter((position) => position.status === 'CLOSED'),
      ),
    ]
  }

  const nextPositions = [...positions]
  nextPositions[existingIndex] = positionUpdate
  return [
    ...sortOpenPositions(
      nextPositions.filter((position) => position.status === 'OPEN'),
    ),
    ...sortClosedPositions(
      nextPositions.filter((position) => position.status === 'CLOSED'),
    ),
  ]
}

export function patchOrder(
  orders: OrderResponse[],
  orderUpdate: OrderResponse,
  limit = orders.length,
) {
  const existingIndex = orders.findIndex((order) => order.id === orderUpdate.id)

  if (existingIndex === -1) {
    return sortOrders([orderUpdate, ...orders]).slice(0, limit)
  }

  const nextOrders = [...orders]
  nextOrders[existingIndex] = orderUpdate
  return sortOrders(nextOrders)
}

export function sortOrders(orders: OrderResponse[]) {
  return [...orders].sort(
    (left, right) =>
      new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime(),
  )
}

export function sortOpenPositions(positions: PositionResponse[]) {
  return [...positions].sort(
    (left, right) =>
      new Date(right.openedAt).getTime() - new Date(left.openedAt).getTime(),
  )
}

export function sortClosedPositions(positions: PositionResponse[]) {
  return [...positions].sort(
    (left, right) =>
      getTimeOrZero(right.closedAt) - getTimeOrZero(left.closedAt),
  )
}

function getTimeOrZero(value: string | null) {
  return value ? new Date(value).getTime() : 0
}
