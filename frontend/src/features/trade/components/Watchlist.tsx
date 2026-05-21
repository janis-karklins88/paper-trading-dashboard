import { Check, Plus, Search, Trash2, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { formatOptionalPrice } from '../../../utils/formatters'
import { getLatestPrices, type MarketPrice } from '../api/marketDataApi'
import { searchSymbols, type SymbolSearchResult } from '../api/symbolApi'
import {
  addWatchlistItem,
  createWatchlist,
  deleteWatchlist,
  getWatchlist,
  getWatchlists,
  removeWatchlistItem,
  type WatchlistDetailResponse,
  type WatchlistResponse,
} from '../api/watchlistApi'

type WatchlistProps = {
  selectedSymbol: string
  onSelectSymbol: (symbol: string) => void
}

const PRICE_REFRESH_MS = 15_000
const MIN_SEARCH_LENGTH = 1

export function Watchlist({
  selectedSymbol,
  onSelectSymbol,
}: WatchlistProps) {
  const [watchlists, setWatchlists] = useState<WatchlistResponse[]>([])
  const [selectedWatchlistId, setSelectedWatchlistId] = useState('')
  const [watchlistDetail, setWatchlistDetail] =
    useState<WatchlistDetailResponse | null>(null)
  const [prices, setPrices] = useState<Record<string, MarketPrice>>({})
  const [newWatchlistName, setNewWatchlistName] = useState('')
  const [isCreatingWatchlist, setIsCreatingWatchlist] = useState(false)
  const [symbolQuery, setSymbolQuery] = useState('')
  const [symbolResults, setSymbolResults] = useState<SymbolSearchResult[]>([])
  const [isLoadingWatchlists, setIsLoadingWatchlists] = useState(true)
  const [isLoadingDetail, setIsLoadingDetail] = useState(false)
  const [isSearchingSymbols, setIsSearchingSymbols] = useState(false)
  const [error, setError] = useState('')

  const selectedWatchlist = useMemo(
    () => watchlists.find((watchlist) => watchlist.id === selectedWatchlistId),
    [selectedWatchlistId, watchlists],
  )

  const itemSymbols = useMemo(
    () => watchlistDetail?.items.map((item) => item.symbol) ?? [],
    [watchlistDetail],
  )

  useEffect(() => {
    let isMounted = true

    getWatchlists()
      .then((nextWatchlists) => {
        if (!isMounted) {
          return
        }

        setWatchlists(nextWatchlists)
        setSelectedWatchlistId(nextWatchlists[0]?.id ?? '')
        setError('')
      })
      .catch((caughtError) => {
        if (isMounted) {
          setError(toErrorMessage(caughtError, 'Failed to load watchlists'))
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoadingWatchlists(false)
        }
      })

    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    if (!selectedWatchlistId) {
      setWatchlistDetail(null)
      return
    }

    let isMounted = true
    setIsLoadingDetail(true)

    getWatchlist(selectedWatchlistId)
      .then((detail) => {
        if (!isMounted) {
          return
        }

        setWatchlistDetail(detail)
        setError('')

        if (!selectedSymbol && detail.items[0]) {
          onSelectSymbol(detail.items[0].symbol)
        }
      })
      .catch((caughtError) => {
        if (isMounted) {
          setWatchlistDetail(null)
          setError(toErrorMessage(caughtError, 'Failed to load watchlist'))
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoadingDetail(false)
        }
      })

    return () => {
      isMounted = false
    }
  }, [onSelectSymbol, selectedWatchlistId])

  useEffect(() => {
    if (itemSymbols.length === 0) {
      setPrices({})
      return
    }

    let isMounted = true

    const loadPrices = () => {
      getLatestPrices(itemSymbols)
        .then((nextPrices) => {
          if (!isMounted) {
            return
          }

          setPrices(
            Object.fromEntries(
              nextPrices.map((price) => [price.symbol, price]),
            ),
          )
        })
        .catch(() => {
          if (isMounted) {
            setPrices({})
          }
        })
    }

    loadPrices()
    const intervalId = window.setInterval(loadPrices, PRICE_REFRESH_MS)

    return () => {
      isMounted = false
      window.clearInterval(intervalId)
    }
  }, [itemSymbols])

  useEffect(() => {
    const normalizedQuery = symbolQuery.trim()

    if (normalizedQuery.length < MIN_SEARCH_LENGTH) {
      setSymbolResults([])
      setIsSearchingSymbols(false)
      return
    }

    let isMounted = true
    setIsSearchingSymbols(true)

    const timeoutId = window.setTimeout(() => {
      searchSymbols(normalizedQuery)
        .then((response) => {
          if (isMounted) {
            setSymbolResults(response.content)
          }
        })
        .catch(() => {
          if (isMounted) {
            setSymbolResults([])
          }
        })
        .finally(() => {
          if (isMounted) {
            setIsSearchingSymbols(false)
          }
        })
    }, 250)

    return () => {
      isMounted = false
      window.clearTimeout(timeoutId)
    }
  }, [symbolQuery])

  async function refreshWatchlists(nextSelectedWatchlistId?: string) {
    const nextWatchlists = await getWatchlists()
    setWatchlists(nextWatchlists)
    setSelectedWatchlistId(
      nextSelectedWatchlistId ?? nextWatchlists[0]?.id ?? '',
    )
  }

  async function refreshSelectedWatchlist() {
    if (!selectedWatchlistId) {
      return
    }

    setWatchlistDetail(await getWatchlist(selectedWatchlistId))
  }

  async function handleCreateWatchlist() {
    const name = newWatchlistName.trim()

    if (!name) {
      return
    }

    try {
      const watchlist = await createWatchlist(name)
      setNewWatchlistName('')
      setIsCreatingWatchlist(false)
      await refreshWatchlists(watchlist.id)
      setError('')
    } catch (caughtError) {
      setError(toErrorMessage(caughtError, 'Failed to create watchlist'))
    }
  }

  async function handleDeleteWatchlist() {
    if (!selectedWatchlist || selectedWatchlist.defaultWatchlist) {
      return
    }

    try {
      await deleteWatchlist(selectedWatchlist.id)
      setWatchlistDetail(null)
      await refreshWatchlists()
      setError('')
    } catch (caughtError) {
      setError(toErrorMessage(caughtError, 'Failed to delete watchlist'))
    }
  }

  async function handleAddSymbol(symbol: string) {
    if (!selectedWatchlistId) {
      return
    }

    try {
      await addWatchlistItem(selectedWatchlistId, symbol)
      setSymbolQuery('')
      setSymbolResults([])
      await refreshSelectedWatchlist()
      onSelectSymbol(symbol)
      setError('')
    } catch (caughtError) {
      setError(toErrorMessage(caughtError, 'Failed to add symbol'))
    }
  }

  async function handleRemoveItem(itemId: string) {
    if (!selectedWatchlistId) {
      return
    }

    try {
      await removeWatchlistItem(selectedWatchlistId, itemId)
      await refreshSelectedWatchlist()
      setError('')
    } catch (caughtError) {
      setError(toErrorMessage(caughtError, 'Failed to remove symbol'))
    }
  }

  return (
    <section className="rounded-lg border border-[#21304a] bg-[#121b2d]/90 p-5 shadow-[0_18px_50px_rgba(3,8,20,0.22)]">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-sm text-[#9db2d0]">Watchlist</h2>
        {!selectedWatchlist?.defaultWatchlist && selectedWatchlist && (
          <button
            aria-label="Delete watchlist"
            className="grid size-9 place-items-center rounded-md border border-[#3a2532] bg-[#ff5367]/10 text-[#ff8a98] transition hover:bg-[#ff5367]/20"
            onClick={handleDeleteWatchlist}
            type="button"
          >
            <Trash2 size={16} />
          </button>
        )}
      </div>

      <div className="mb-4 grid gap-3">
        <div className="grid grid-cols-[1fr_auto] gap-2">
          <select
            className="min-h-10 min-w-0 rounded-md border border-[#21304a] bg-[#0f1727] px-3 text-sm font-semibold text-[#eef4ff] outline-none"
            disabled={isLoadingWatchlists || watchlists.length === 0}
            onChange={(event) => setSelectedWatchlistId(event.target.value)}
            value={selectedWatchlistId}
          >
            {watchlists.map((watchlist) => (
              <option key={watchlist.id} value={watchlist.id}>
                {watchlist.name}
              </option>
            ))}
          </select>
          <button
            aria-label="Create watchlist"
            className="grid size-10 place-items-center rounded-md border border-[#214938] bg-[#00d084]/12 text-[#00d084] transition hover:bg-[#00d084]/20"
            onClick={() => setIsCreatingWatchlist(true)}
            type="button"
          >
            <Plus size={18} />
          </button>
        </div>

        {isCreatingWatchlist && (
          <div className="grid grid-cols-[1fr_auto_auto] gap-2">
            <input
              autoFocus
              className="min-h-10 min-w-0 rounded-md border border-[#21304a] bg-[#0f1727] px-3 text-sm font-semibold text-[#eef4ff] outline-none placeholder:text-[#6f829f]"
              onChange={(event) => setNewWatchlistName(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  void handleCreateWatchlist()
                }

                if (event.key === 'Escape') {
                  setIsCreatingWatchlist(false)
                  setNewWatchlistName('')
                }
              }}
              placeholder="Watchlist name"
              type="text"
              value={newWatchlistName}
            />
            <button
              aria-label="Confirm watchlist"
              className="grid size-10 place-items-center rounded-md bg-[#2fac7e] text-[#061a14] transition hover:bg-[#22ffb2] disabled:cursor-not-allowed disabled:opacity-50"
              disabled={!newWatchlistName.trim()}
              onClick={handleCreateWatchlist}
              type="button"
            >
              <Check size={18} />
            </button>
            <button
              aria-label="Cancel watchlist"
              className="grid size-10 place-items-center rounded-md border border-[#21304a] bg-[#0f1727] text-[#9db2d0] transition hover:text-white"
              onClick={() => {
                setIsCreatingWatchlist(false)
                setNewWatchlistName('')
              }}
              type="button"
            >
              <X size={18} />
            </button>
          </div>
        )}
      </div>

      <div className="mb-4 grid gap-2">
        <div className="relative">
          <Search
            className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[#6f829f]"
            size={16}
          />
          <input
            className="min-h-10 w-full rounded-md border border-[#21304a] bg-[#0f1727] px-9 text-sm font-semibold text-[#eef4ff] outline-none placeholder:text-[#6f829f]"
            disabled={!selectedWatchlistId}
            onChange={(event) => setSymbolQuery(event.target.value)}
            placeholder="Add symbol"
            type="text"
            value={symbolQuery}
          />
          {symbolQuery && (
            <button
              aria-label="Clear symbol search"
              className="absolute right-2 top-1/2 grid size-7 -translate-y-1/2 place-items-center rounded text-[#9db2d0] hover:text-white"
              onClick={() => setSymbolQuery('')}
              type="button"
            >
              <X size={15} />
            </button>
          )}
        </div>

        {symbolQuery.trim().length >= MIN_SEARCH_LENGTH && (
          <div className="max-h-56 overflow-auto rounded-md border border-[#21304a] bg-[#0f1727]">
            {isSearchingSymbols && (
              <p className="px-3 py-2.5 text-sm font-semibold text-[#9db2d0]">
                Searching
              </p>
            )}

            {!isSearchingSymbols && symbolResults.length === 0 && (
              <p className="px-3 py-2.5 text-sm font-semibold text-[#9db2d0]">
                No matches
              </p>
            )}

            {!isSearchingSymbols &&
              symbolResults.map((result) => (
                <button
                  className="grid w-full grid-cols-[1fr_auto] items-center gap-3 px-3 py-2.5 text-left transition hover:bg-[#18243a]"
                  key={result.id}
                  onClick={() => void handleAddSymbol(result.symbol)}
                  type="button"
                >
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-bold text-[#f7fbff]">
                      {result.symbol}
                    </span>
                    <span className="block truncate text-xs text-[#9db2d0]">
                      {result.displayName}
                    </span>
                  </span>
                  <Plus className="text-[#00d084]" size={16} />
                </button>
              ))}
          </div>
        )}
      </div>

      {error && (
        <p className="mb-4 rounded-md bg-[#ff5367]/12 px-3 py-2.5 text-sm font-bold text-[#ffdce1]">
          {error}
        </p>
      )}

      <div className="watchlist-scroll max-h-[34rem] overflow-y-auto overflow-x-hidden rounded-md border border-[#1e293b] bg-[#0f1727]">
        {isLoadingDetail && (
          <p className="px-3 py-2 text-sm font-semibold text-[#9db2d0]">
            Loading
          </p>
        )}

        {!isLoadingDetail &&
          watchlistDetail?.items.map((item) => {
            const price = prices[item.symbol]
            const isSelected = item.symbol === selectedSymbol

            return (
              <div
                className={[
                  'grid min-h-9 grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-2 border-b border-[#1e293b] px-3 py-1.5 text-sm transition last:border-b-0',
                  isSelected
                    ? 'bg-[#18234d] text-[#f7fbff]'
                    : 'bg-[#0f1727] text-[#dce8ff]',
                ].join(' ')}
                key={item.id}
              >
                <button
                  className="min-w-0 truncate text-left font-semibold"
                  onClick={() => onSelectSymbol(item.symbol)}
                  type="button"
                >
                  {item.symbol}
                </button>

                <span className="font-semibold tabular-nums text-[#f7fbff]">
                  {formatOptionalPrice(price?.price)}
                </span>

                <button
                  aria-label={`Remove ${item.symbol}`}
                  className="grid size-6 place-items-center rounded text-[#6f829f] transition hover:bg-[#ff5367]/12 hover:text-[#ff8a98]"
                  onClick={() => void handleRemoveItem(item.id)}
                  type="button"
                >
                  <X size={13} />
                </button>
              </div>
            )
          })}

        {!isLoadingDetail && watchlistDetail?.items.length === 0 && (
          <p className="px-3 py-2 text-sm font-semibold text-[#9db2d0]">
            No symbols
          </p>
        )}
      </div>
    </section>
  )
}

function toErrorMessage(caughtError: unknown, fallback: string) {
  return caughtError instanceof Error ? caughtError.message : fallback
}
