import type { StockTableRow } from '../../types/stock'
import { formatCurrency, formatPercent, getReturnColorClass } from '../../utils/stockCalculations'

interface PortfolioTableRowProps {
  row: StockTableRow
}

const PortfolioTableRow = ({ row }: PortfolioTableRowProps) => {
  const returnColorClass = getReturnColorClass(row.totalReturnDollars)

  return (
    <tr className="border-b border-slate-100 hover:bg-slate-50 transition-colors">
      {/* Symbol Column */}
      <td className="px-6 py-4">
        <div className="flex flex-col">
          <span className="text-slate-900 font-semibold text-base">{row.symbol}</span>
          <span className="text-slate-500 text-sm">{row.companyName}</span>
        </div>
      </td>

      {/* Last Price Column */}
      <td className="px-6 py-4">
        <span className="text-slate-900 font-medium">{formatCurrency(row.lastPrice)}</span>
      </td>

      {/* Total Return Column */}
      <td className="px-6 py-4">
        <div className="flex flex-col">
          <span className={`font-semibold ${returnColorClass}`}>
            {formatPercent(row.totalReturnPercent)}
          </span>
          <span className={`text-sm ${returnColorClass}`}>
            {formatCurrency(row.totalReturnDollars)}
          </span>
        </div>
      </td>

      {/* Value/Cost Column */}
      <td className="px-6 py-4">
        <div className="flex flex-col">
          <span className="text-slate-900 font-semibold">{formatCurrency(row.currentValue)}</span>
          <span className="text-slate-500 text-sm">{formatCurrency(row.costBasis)}</span>
        </div>
      </td>
    </tr>
  )
}

export default PortfolioTableRow
