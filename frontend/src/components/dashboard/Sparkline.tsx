import { useMemo } from 'react'
import { LineChart, Line, ResponsiveContainer } from 'recharts'

interface SparklineProps {
  data: number[]
  width?: number
  height?: number
}

export function Sparkline({ data, width = 80, height = 32 }: SparklineProps) {
  const chartData = useMemo(() => data.map((value, index) => ({ index, value })), [data])

  // Determine color based on trend (first vs last value)
  const isPositive = data.length >= 2 && data[data.length - 1] >= data[0]
  const strokeColor = isPositive ? '#10b981' : '#ef4444'

  if (data.length < 2) {
    return <div className="w-20 h-8 bg-slate-100 rounded" />
  }

  return (
    <div style={{ width, height }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData}>
          <Line
            type="monotone"
            dataKey="value"
            stroke={strokeColor}
            strokeWidth={1.5}
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
