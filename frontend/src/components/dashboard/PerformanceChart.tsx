import { useMemo } from 'react'
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts'
import type { TimeRange, PortfolioPerformancePoint } from '../../services/api/portfolioApi'
import { formatCurrency } from '../../utils/stockFormatters'
import { useBreakpoint } from '../../hooks/useBreakpoint'

interface PerformanceChartProps {
  data: PortfolioPerformancePoint[]
  range: TimeRange
  onRangeChange: (range: TimeRange) => void
  loading?: boolean
}

const TIME_RANGES: { value: TimeRange; label: string }[] = [
  { value: '7d', label: '7D' },
  { value: '1mo', label: '1M' },
  { value: '3mo', label: '3M' },
  { value: 'ytd', label: 'YTD' },
  { value: '1y', label: '1Y' },
]

export function PerformanceChart({ data, range, onRangeChange, loading }: PerformanceChartProps) {
  const { isMobile } = useBreakpoint()

  // Format date based on range
  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr)
    if (range === '7d') {
      return date.toLocaleDateString('en-US', { weekday: 'short' })
    }
    if (range === '1mo' || range === '3mo') {
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    }
    return date.toLocaleDateString('en-US', { month: 'short', year: '2-digit' })
  }

  // Calculate period return
  const periodReturn = useMemo(() => {
    if (data.length < 2) return { dollars: 0, percent: 0 }
    const first = data[0].totalValue
    const last = data[data.length - 1].totalValue
    const dollars = last - first
    const percent = first > 0 ? (dollars / first) * 100 : 0
    return { dollars, percent }
  }, [data])

  const isPositive = periodReturn.dollars >= 0
  const chartColor = isPositive ? '#10b981' : '#ef4444' // emerald-500 / red-500
  const gradientId = 'performanceGradient'

  return (
    <div className="rounded-xl border border-border bg-white p-4 sm:p-6 shadow-soft">
      {/* Header with range toggles */}
      <div className="mb-4 sm:mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-base sm:text-lg font-semibold text-slate-900">Portfolio Performance</h2>
          <div className={`text-xl sm:text-2xl font-bold ${isPositive ? 'text-emerald-600' : 'text-red-500'}`}>
            {isPositive ? '+' : ''}
            {formatCurrency(periodReturn.dollars)}
            <span className="ml-1 sm:ml-2 text-sm sm:text-base font-medium">
              ({isPositive ? '+' : ''}
              {periodReturn.percent.toFixed(2)}%)
            </span>
          </div>
        </div>

        {/* Time range toggle buttons - scrollable on mobile */}
        <div className="flex gap-1 rounded-lg bg-slate-100 p-1 overflow-x-auto">
          {TIME_RANGES.map(({ value, label }) => (
            <button
              key={value}
              onClick={() => onRangeChange(value)}
              className={`flex-shrink-0 px-2.5 sm:px-3 py-1 sm:py-1.5 text-xs sm:text-sm font-medium rounded-md transition-colors
                ${
                  range === value
                    ? 'bg-white text-slate-900 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* Chart */}
      {loading ? (
        <div className="flex h-48 sm:h-64 items-center justify-center">
          <span className="text-slate-500">Loading chart...</span>
        </div>
      ) : (
        <div className="h-48 sm:h-64 md:h-72">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor={chartColor} stopOpacity={0.3} />
                  <stop offset="100%" stopColor={chartColor} stopOpacity={0.05} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
              <XAxis
                dataKey="date"
                tickFormatter={formatDate}
                tick={{ fontSize: isMobile ? 10 : 12, fill: '#64748b' }}
                axisLine={{ stroke: '#e2e8f0' }}
                tickLine={false}
              />
              <YAxis
                tickFormatter={value => `$${(value / 1000).toFixed(0)}k`}
                tick={{ fontSize: isMobile ? 10 : 12, fill: '#64748b' }}
                axisLine={false}
                tickLine={false}
                width={isMobile ? 45 : 60}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: 'white',
                  border: '1px solid #e2e8f0',
                  borderRadius: '8px',
                  boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
                }}
                formatter={(value: number | undefined) => [
                  formatCurrency(value || 0),
                  'Portfolio Value',
                ]}
                labelFormatter={label =>
                  new Date(label).toLocaleDateString('en-US', {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  })
                }
              />
              <Area
                type="monotone"
                dataKey="totalValue"
                stroke={chartColor}
                strokeWidth={2}
                fill={`url(#${gradientId})`}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}
