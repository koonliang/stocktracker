import type { SortDirection } from '../../hooks/useSort'

interface SortableHeaderProps {
  label: string
  sortKey: string
  currentSortKey: string | null
  sortDirection: SortDirection
  onSort: (key: string) => void
  className?: string
  align?: 'left' | 'right' | 'center'
}

export function SortableHeader({
  label,
  sortKey,
  currentSortKey,
  sortDirection,
  onSort,
  className = '',
  align = 'left',
}: SortableHeaderProps) {
  const isActive = currentSortKey === sortKey
  const alignClass =
    align === 'right' ? 'justify-end' : align === 'center' ? 'justify-center' : 'justify-start'

  return (
    <th
      className={`px-4 lg:px-6 py-4 text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600 cursor-pointer hover:bg-slate-100 transition-colors select-none ${className}`}
      onClick={() => onSort(sortKey)}
    >
      <div className={`flex items-center gap-1 ${alignClass}`}>
        <span>{label}</span>
        <span className={`text-xs ${isActive ? 'text-indigo-600' : 'text-slate-400'}`}>
          {isActive ? (sortDirection === 'asc' ? '↑' : '↓') : '↕'}
        </span>
      </div>
    </th>
  )
}
