import type { HoldingResponse } from '../../services/api/portfolioApi'
import { formatCurrency, formatPercent, getReturnColorClass } from '../../utils/stockFormatters'
import { Sparkline } from './Sparkline'

interface HoldingCardProps {
  holding: HoldingResponse
}

export function HoldingCard({ holding }: HoldingCardProps) {
  const returnClass = getReturnColorClass(holding.totalReturnDollars)
  const sevenDayReturnClass = getReturnColorClass(holding.sevenDayReturnDollars)

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-soft">
      {/* Header: Symbol & Company */}
      <div className="flex items-start justify-between mb-3">
        <div>
          <div className="font-semibold text-slate-900 text-lg">{holding.symbol}</div>
          <div className="text-sm text-slate-500 truncate max-w-[180px]">{holding.companyName}</div>
        </div>
        <div className="text-right">
          <div className="font-semibold text-slate-900">{formatCurrency(holding.currentValue)}</div>
          <div className={`text-sm font-medium ${returnClass}`}>
            {formatPercent(holding.totalReturnPercent)}
          </div>
        </div>
      </div>

      {/* Sparkline */}
      <div className="mb-3 flex justify-center">
        <Sparkline data={holding.sparklineData} width={280} height={40} />
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-2 gap-3 text-sm">
        <div className="flex justify-between">
          <span className="text-slate-500">Last Price</span>
          <span className="font-medium text-slate-900">{formatCurrency(holding.lastPrice)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-slate-500">Avg Cost</span>
          <span className="font-medium text-slate-900">{formatCurrency(holding.averageCost)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-slate-500">7D Return</span>
          <span className={`font-medium ${sevenDayReturnClass}`}>
            {formatPercent(holding.sevenDayReturnPercent)}
          </span>
        </div>
        <div className="flex justify-between">
          <span className="text-slate-500">Total Return</span>
          <span className={`font-medium ${returnClass}`}>
            {formatCurrency(holding.totalReturnDollars)}
          </span>
        </div>
        <div className="flex justify-between">
          <span className="text-slate-500">Shares</span>
          <span className="font-medium text-slate-900">{holding.shares.toFixed(2)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-slate-500">Weight</span>
          <span className="font-medium text-slate-900">{holding.weight.toFixed(1)}%</span>
        </div>
        <div className="flex justify-between col-span-2 pt-2 border-t border-slate-100">
          <span className="text-slate-500">Cost Basis</span>
          <span className="font-medium text-slate-700">{formatCurrency(holding.costBasis)}</span>
        </div>
      </div>
    </div>
  )
}
