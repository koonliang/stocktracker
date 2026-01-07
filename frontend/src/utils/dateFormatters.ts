/**
 * Format a UTC timestamp to local time with clear indication.
 * Example: "2:30:45 PM (local time)"
 */
export function formatLocalTime(utcTimestamp: string | null | undefined): string {
  if (!utcTimestamp) return '-'

  const date = new Date(utcTimestamp)

  // Check if valid date
  if (isNaN(date.getTime())) return '-'

  return date.toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: true,
  })
}

/**
 * Format timestamp with relative time indicator.
 * Example: "2:30 PM (3 min ago)"
 */
export function formatTimeWithRelative(utcTimestamp: string | null | undefined): string {
  if (!utcTimestamp) return '-'

  const date = new Date(utcTimestamp)
  if (isNaN(date.getTime())) return '-'

  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)

  const timeStr = date.toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  })

  if (diffMins < 1) {
    return `${timeStr} (just now)`
  } else if (diffMins < 60) {
    return `${timeStr} (${diffMins} min ago)`
  } else {
    return timeStr
  }
}
