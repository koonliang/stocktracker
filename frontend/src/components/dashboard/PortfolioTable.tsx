import type { HoldingResponse } from '../../services/api/portfolioApi'
import { PortfolioTableRow } from './PortfolioTableRow'
import { HoldingCard } from './HoldingCard'
import { SortableHeader } from './SortableHeader'
import { useSort } from '../../hooks/useSort'

interface PortfolioTableProps {
  holdings: HoldingResponse[]
}

type SortKey = keyof HoldingResponse

export function PortfolioTable({ holdings }: PortfolioTableProps) {
  const { sortedData, sortConfig, requestSort } = useSort<HoldingResponse>(holdings, 'symbol')

  return (
    <>
      {/* Mobile Card View - with sorted data */}
      <div className="md:hidden space-y-4">
        {sortedData.map(holding => (
          <HoldingCard key={holding.id} holding={holding} />
        ))}
      </div>

      {/* Desktop Table View */}
      <div className="hidden md:block overflow-x-auto rounded-xl border border-border bg-white shadow-soft">
        <table className="w-full">
          <thead className="bg-slate-50">
            <tr>
              <SortableHeader
                label="Symbol"
                sortKey="symbol"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={key => requestSort(key as SortKey)}
                align="left"
              />
              <SortableHeader
                label="Last Price"
                sortKey="lastPrice"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={key => requestSort(key as SortKey)}
                align="right"
              />
              <SortableHeader
                label="7D Return"
                sortKey="sevenDayReturnPercent"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={key => requestSort(key as SortKey)}
                align="right"
              />
              <SortableHeader
                label="Total Return"
                sortKey="totalReturnPercent"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={key => requestSort(key as SortKey)}
                align="right"
              />
              <SortableHeader
                label="Value / Cost"
                sortKey="currentValue"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={key => requestSort(key as SortKey)}
                align="right"
              />
              <SortableHeader
                label="Weight / Shares"
                sortKey="weight"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={key => requestSort(key as SortKey)}
                align="right"
                className="hidden lg:table-cell"
              />
              <SortableHeader
                label="Avg Price"
                sortKey="averageCost"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={key => requestSort(key as SortKey)}
                align="right"
                className="hidden lg:table-cell"
              />
              <th className="hidden xl:table-cell px-4 lg:px-6 py-4 text-center text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                1Y Chart
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {sortedData.map(holding => (
              <PortfolioTableRow key={holding.id} holding={holding} />
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
