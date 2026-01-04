export interface TransactionFiltersProps {
  symbols: string[]
  filterSymbol: string
  filterType: 'ALL' | 'BUY' | 'SELL'
  searchQuery: string
  onFilterSymbolChange: (symbol: string) => void
  onFilterTypeChange: (type: 'ALL' | 'BUY' | 'SELL') => void
  onSearchChange: (query: string) => void
}

export function TransactionFilters({
  symbols,
  filterSymbol,
  filterType,
  searchQuery,
  onFilterSymbolChange,
  onFilterTypeChange,
  onSearchChange,
}: TransactionFiltersProps) {
  return (
    <div className="flex flex-col sm:flex-row gap-3">
      {/* Symbol Filter */}
      <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-2">
        <label
          htmlFor="symbol-filter"
          className="text-sm font-medium text-slate-700 whitespace-nowrap"
        >
          Symbol:
        </label>
        <select
          id="symbol-filter"
          value={filterSymbol}
          onChange={e => onFilterSymbolChange(e.target.value)}
          className="min-w-[120px] rounded-lg border border-slate-300 px-3 py-2 text-sm
                   focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500
                   bg-white"
        >
          <option value="">All</option>
          {symbols.map(symbol => (
            <option key={symbol} value={symbol}>
              {symbol}
            </option>
          ))}
        </select>
      </div>

      {/* Type Filter */}
      <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-2">
        <label
          htmlFor="type-filter"
          className="text-sm font-medium text-slate-700 whitespace-nowrap"
        >
          Type:
        </label>
        <select
          id="type-filter"
          value={filterType}
          onChange={e => onFilterTypeChange(e.target.value as 'ALL' | 'BUY' | 'SELL')}
          className="min-w-[120px] rounded-lg border border-slate-300 px-3 py-2 text-sm
                   focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500
                   bg-white"
        >
          <option value="ALL">All</option>
          <option value="BUY">Buy</option>
          <option value="SELL">Sell</option>
        </select>
      </div>

      {/* Search Input */}
      <div className="flex-1">
        <label htmlFor="search-filter" className="sr-only">
          Search transactions
        </label>
        <div className="relative">
          <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
            <svg
              className="h-4 w-4 text-slate-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
          </div>
          <input
            id="search-filter"
            type="text"
            value={searchQuery}
            onChange={e => onSearchChange(e.target.value)}
            placeholder="Search transactions..."
            className="block w-full rounded-lg border border-slate-300 pl-10 pr-3 py-2 text-sm
                     focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
      </div>
    </div>
  )
}
