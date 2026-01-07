import { useState, useMemo, useCallback } from 'react'

export type SortDirection = 'asc' | 'desc'

export interface SortConfig<T> {
  key: keyof T | null
  direction: SortDirection
}

export function useSort<T>(data: T[], defaultKey: keyof T | null = null) {
  const [sortConfig, setSortConfig] = useState<SortConfig<T>>({
    key: defaultKey,
    direction: 'asc',
  })

  const sortedData = useMemo(() => {
    if (!sortConfig.key) return data

    return [...data].sort((a, b) => {
      const aValue = a[sortConfig.key!]
      const bValue = b[sortConfig.key!]

      // Handle null/undefined
      if (aValue == null && bValue == null) return 0
      if (aValue == null) return sortConfig.direction === 'asc' ? 1 : -1
      if (bValue == null) return sortConfig.direction === 'asc' ? -1 : 1

      // Compare values
      let comparison = 0
      if (typeof aValue === 'string' && typeof bValue === 'string') {
        comparison = aValue.localeCompare(bValue)
      } else if (typeof aValue === 'number' && typeof bValue === 'number') {
        comparison = aValue - bValue
      }

      return sortConfig.direction === 'asc' ? comparison : -comparison
    })
  }, [data, sortConfig])

  const requestSort = useCallback((key: keyof T) => {
    setSortConfig(prev => ({
      key,
      direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc',
    }))
  }, [])

  const resetSort = useCallback(() => {
    setSortConfig({ key: defaultKey, direction: 'asc' })
  }, [defaultKey])

  return { sortedData, sortConfig, requestSort, resetSort }
}
