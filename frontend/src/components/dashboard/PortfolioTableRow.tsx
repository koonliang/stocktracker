import type { HoldingResponse } from '../../services/api/portfolioApi'
import { formatCurrency, formatPercent, getReturnColorClass } from '../../utils/stockFormatters'
import { Sparkline } from './Sparkline'

interface PortfolioTableRowProps {
  holding: HoldingResponse
}

export function PortfolioTableRow({ holding }: PortfolioTableRowProps) {
  const returnClass = getReturnColorClass(holding.totalReturnDollars)
  const sevenDayReturnClass = getReturnColorClass(holding.sevenDayReturnDollars)

  return (
    <tr className="transition-colors hover:bg-slate-50">
      {/* Symbol Column */}
      <td className="px-4 lg:px-6 py-4">
        <div className="font-semibold text-slate-900">{holding.symbol}</div>
        <div className="text-sm text-slate-500">{holding.companyName}</div>
      </td>

      {/* Last Price Column */}
      <td className="px-4 lg:px-6 py-4 text-right font-medium text-slate-900">
        {formatCurrency(holding.lastPrice)}
      </td>

      {/* 7D Return Column */}
      <td className={`px-4 lg:px-6 py-4 text-right ${sevenDayReturnClass}`}>
        <div className="font-medium">{formatPercent(holding.sevenDayReturnPercent)}</div>
      </td>

      {/* Total Return Column */}
      <td className={`px-4 lg:px-6 py-4 text-right ${returnClass}`}>
        <div className="font-medium">{formatPercent(holding.totalReturnPercent)}</div>
        <div className="text-sm">{formatCurrency(holding.totalReturnDollars)}</div>
      </td>

      {/* Value/Cost Column */}
      <td className="px-4 lg:px-6 py-4 text-right">
        <span className="font-semibold text-slate-900">{formatCurrency(holding.currentValue)}</span>
        <span className="text-slate-400"> / </span>
        <span className="text-slate-500">{formatCurrency(holding.costBasis)}</span>
      </td>

      {/* Weight/Shares Column - hidden on tablet */}
      <td className="hidden lg:table-cell px-4 lg:px-6 py-4 text-right">
        <div className="font-medium text-slate-900">{holding.weight.toFixed(1)}%</div>
        <div className="text-sm text-slate-500">{holding.shares.toFixed(2)} shares</div>
      </td>

      {/* Avg Price Column - hidden on tablet */}
      <td className="hidden lg:table-cell px-4 lg:px-6 py-4 text-right font-medium text-slate-900">
        {formatCurrency(holding.averageCost)}
      </td>

      {/* 1Y Chart Column - hidden until xl */}
      <td className="hidden xl:table-cell px-4 lg:px-6 py-4">
        <div className="flex justify-center">
          <Sparkline data={holding.sparklineData} />
        </div>
      </td>
    </tr>
  )
}
