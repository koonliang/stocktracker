import { useEffect } from 'react'

/**
 * Hook to prevent body scroll when active (e.g., when modal is open)
 * Also accounts for scrollbar width to prevent layout shift
 */
export function useBodyScrollLock(isLocked: boolean) {
  useEffect(() => {
    if (isLocked) {
      // Calculate scrollbar width to prevent layout shift
      const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth

      // Lock scroll and add padding to compensate for scrollbar
      document.body.style.overflow = 'hidden'
      document.body.style.paddingRight = `${scrollbarWidth}px`
    } else {
      // Restore normal scroll
      document.body.style.overflow = ''
      document.body.style.paddingRight = ''
    }

    // Cleanup on unmount
    return () => {
      document.body.style.overflow = ''
      document.body.style.paddingRight = ''
    }
  }, [isLocked])
}
