import type { HoldingResponse } from '../../services/api/portfolioApi';
import { formatCurrency, formatPercent, getReturnColorClass } from '../../utils/stockFormatters';

interface PortfolioTableRowProps {
  holding: HoldingResponse;
}

export function PortfolioTableRow({ holding }: PortfolioTableRowProps) {
  const returnClass = getReturnColorClass(holding.totalReturnDollars);

  return (
    <tr className="transition-colors hover:bg-slate-50">
      {/* Symbol Column */}
      <td className="px-6 py-4">
        <div className="font-semibold text-slate-900">{holding.symbol}</div>
        <div className="text-sm text-slate-500">{holding.companyName}</div>
      </td>

      {/* Last Price Column */}
      <td className="px-6 py-4 text-right font-medium text-slate-900">
        {formatCurrency(holding.lastPrice)}
      </td>

      {/* Total Return Column */}
      <td className={`px-6 py-4 text-right ${returnClass}`}>
        <div className="font-medium">{formatPercent(holding.totalReturnPercent)}</div>
        <div className="text-sm">{formatCurrency(holding.totalReturnDollars)}</div>
      </td>

      {/* Value/Cost Column */}
      <td className="px-6 py-4 text-right">
        <span className="font-semibold text-slate-900">
          {formatCurrency(holding.currentValue)}
        </span>
        <span className="text-slate-400"> / </span>
        <span className="text-slate-500">{formatCurrency(holding.costBasis)}</span>
      </td>
    </tr>
  );
}
