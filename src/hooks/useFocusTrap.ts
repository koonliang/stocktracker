import { useRef, useEffect } from 'react'

/**
 * Hook to trap focus within an element for accessibility
 * Useful for modals and dialogs to prevent keyboard navigation from leaving
 */
export function useFocusTrap(isActive: boolean) {
  const elementRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!isActive || !elementRef.current) return

    const element = elementRef.current

    // Get all focusable elements
    const focusableSelector =
      'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    const focusableElements = element.querySelectorAll<HTMLElement>(focusableSelector)

    if (focusableElements.length === 0) return

    const firstElement = focusableElements[0]
    const lastElement = focusableElements[focusableElements.length - 1]

    // Focus first element when trap activates
    firstElement?.focus()

    // Handle Tab key to cycle focus within modal
    const handleTabKey = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return

      if (e.shiftKey) {
        // Shift + Tab: Moving backwards
        if (document.activeElement === firstElement) {
          lastElement?.focus()
          e.preventDefault()
        }
      } else {
        // Tab: Moving forwards
        if (document.activeElement === lastElement) {
          firstElement?.focus()
          e.preventDefault()
        }
      }
    }

    element.addEventListener('keydown', handleTabKey)

    return () => {
      element.removeEventListener('keydown', handleTabKey)
    }
  }, [isActive])

  return elementRef
}
