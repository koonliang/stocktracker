import type { HoldingResponse } from '../../services/api/portfolioApi'
import { PortfolioTableRow } from './PortfolioTableRow'

interface PortfolioTableProps {
  holdings: HoldingResponse[]
}

export function PortfolioTable({ holdings }: PortfolioTableProps) {
  return (
    <div className="overflow-x-auto rounded-xl border border-border bg-white shadow-soft">
      <table className="w-full">
        <thead className="bg-slate-50">
          <tr>
            <th className="px-6 py-4 text-left text-sm font-semibold uppercase tracking-wide text-slate-600">
              Symbol
            </th>
            <th className="px-6 py-4 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
              Last Price
            </th>
            {/* NEW: 7D Return column */}
            <th className="px-6 py-4 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
              7D Return
            </th>
            <th className="px-6 py-4 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
              Total Return
            </th>
            <th className="px-6 py-4 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
              Value / Cost
            </th>
            {/* NEW: Weight/Shares column */}
            <th className="px-6 py-4 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
              Weight / Shares
            </th>
            {/* NEW: Avg Price column */}
            <th className="px-6 py-4 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
              Avg Price
            </th>
            {/* NEW: 1Y Chart column */}
            <th className="px-6 py-4 text-center text-sm font-semibold uppercase tracking-wide text-slate-600">
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
  )
}
