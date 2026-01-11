import { useState, useCallback } from 'react'

/**
 * Hook to manage modal state with animation-aware closing
 * Returns open/close state and handler functions
 */
export function useModal(initialOpen = false) {
  const [isOpen, setIsOpen] = useState(initialOpen)
  const [isClosing, setIsClosing] = useState(false)

  const open = useCallback(() => {
    setIsOpen(true)
    setIsClosing(false)
  }, [])

  const close = useCallback(() => {
    // Trigger closing animation
    setIsClosing(true)

    // Wait for animation to complete before actually closing
    // 150ms matches the fadeIn animation duration from tailwind.config.cjs
    setTimeout(() => {
      setIsOpen(false)
      setIsClosing(false)
    }, 150)
  }, [])

  return { isOpen, isClosing, open, close }
}
