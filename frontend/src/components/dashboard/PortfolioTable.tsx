import type { StockHolding } from '../../types/stock'
import { calculateStockTableRow } from '../../utils/stockCalculations'
import PortfolioTableRow from './PortfolioTableRow'

interface PortfolioTableProps {
  holdings: StockHolding[]
}

const PortfolioTable = ({ holdings }: PortfolioTableProps) => {
  return (
    <div className="bg-white rounded-xl border border-slate-100 shadow-soft overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-6 py-4 text-left text-sm font-semibold text-slate-600 uppercase tracking-wide">
                Symbol
              </th>
              <th className="px-6 py-4 text-left text-sm font-semibold text-slate-600 uppercase tracking-wide">
                Last Price
              </th>
              <th className="px-6 py-4 text-left text-sm font-semibold text-slate-600 uppercase tracking-wide">
                Total Return
              </th>
              <th className="px-6 py-4 text-left text-sm font-semibold text-slate-600 uppercase tracking-wide">
                Value / Cost
              </th>
            </tr>
          </thead>
          <tbody>
            {holdings.map((holding) => {
              const row = calculateStockTableRow(holding)
              return <PortfolioTableRow key={holding.id} row={row} />
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default PortfolioTable
