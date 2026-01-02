import type { HoldingResponse } from '../../services/api/portfolioApi'
import { PortfolioTableRow } from './PortfolioTableRow'
import { HoldingCard } from './HoldingCard'

interface PortfolioTableProps {
  holdings: HoldingResponse[]
}

export function PortfolioTable({ holdings }: PortfolioTableProps) {
  return (
    <>
      {/* Mobile Card View */}
      <div className="md:hidden space-y-4">
        {holdings.map(holding => (
          <HoldingCard key={holding.id} holding={holding} />
        ))}
      </div>

      {/* Desktop Table View */}
      <div className="hidden md:block overflow-x-auto rounded-xl border border-border bg-white shadow-soft">
        <table className="w-full">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-4 lg:px-6 py-4 text-left text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                Symbol
              </th>
              <th className="px-4 lg:px-6 py-4 text-right text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                Last Price
              </th>
              <th className="px-4 lg:px-6 py-4 text-right text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                7D Return
              </th>
              <th className="px-4 lg:px-6 py-4 text-right text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                Total Return
              </th>
              <th className="px-4 lg:px-6 py-4 text-right text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                Value / Cost
              </th>
              <th className="hidden lg:table-cell px-4 lg:px-6 py-4 text-right text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                Weight / Shares
              </th>
              <th className="hidden lg:table-cell px-4 lg:px-6 py-4 text-right text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                Avg Price
              </th>
              <th className="hidden xl:table-cell px-4 lg:px-6 py-4 text-center text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                1Y Chart
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {holdings.map(holding => (
              <PortfolioTableRow key={holding.id} holding={holding} />
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
