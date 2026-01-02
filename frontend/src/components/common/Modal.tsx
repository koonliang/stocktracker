import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { useFocusTrap } from '../../hooks/useFocusTrap'
import { useBodyScrollLock } from '../../hooks/useBodyScrollLock'

export interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  children: React.ReactNode
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full'
  showCloseButton?: boolean
  closeOnEscape?: boolean
  closeOnBackdropClick?: boolean
  isClosing?: boolean
}

export function Modal({
  isOpen,
  onClose,
  title,
  children,
  size = 'lg',
  showCloseButton = true,
  closeOnEscape = true,
  closeOnBackdropClick = true,
  isClosing = false,
}: ModalProps) {
  const focusTrapRef = useFocusTrap(isOpen && !isClosing)
  useBodyScrollLock(isOpen && !isClosing)

  // Handle ESC key to close
  useEffect(() => {
    if (!isOpen || !closeOnEscape) return

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [isOpen, closeOnEscape, onClose])

  // Don't render if not open
  if (!isOpen && !isClosing) return null

  // Handle backdrop click
  const handleBackdropClick = (e: React.MouseEvent) => {
    if (closeOnBackdropClick && e.target === e.currentTarget) {
      onClose()
    }
  }

  // Size classes for responsive modal
  const sizeClasses = {
    sm: 'md:max-w-sm',
    md: 'md:max-w-md',
    lg: 'md:max-w-lg',
    xl: 'md:max-w-4xl',
    full: 'md:max-w-full',
  }

  // Animation classes
  const backdropClasses = `
    fixed inset-0 bg-black/50 transition-opacity duration-150
    ${isClosing ? 'opacity-0' : 'opacity-100'}
  `.trim()

  const modalClasses = `
    fixed inset-0 md:inset-auto md:top-1/2 md:left-1/2
    md:-translate-x-1/2 md:-translate-y-1/2
    ${sizeClasses[size]}
    w-full md:w-auto
    max-h-screen md:max-h-[90vh]
    bg-white
    rounded-none md:rounded-xl
    shadow-2xl
    flex flex-col
    transition-all duration-150
    ${isClosing ? 'opacity-0 scale-95' : 'opacity-100 scale-100'}
  `.trim()

  const modalContent = (
    <>
      {/* Backdrop */}
      <div className={backdropClasses} onClick={handleBackdropClick} aria-hidden="true" />

      {/* Modal Container */}
      <div
        ref={focusTrapRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        className={modalClasses}
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4">
          <h2 id="modal-title" className="text-xl font-semibold text-slate-900">
            {title}
          </h2>
          {showCloseButton && (
            <button
              onClick={onClose}
              className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
              aria-label="Close modal"
            >
              <svg
                className="h-5 w-5"
                fill="none"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          )}
        </div>

        {/* Body */}
        <div className="flex-1 overflow-auto px-6 py-4">{children}</div>
      </div>
    </>
  )

  // Render to body using portal
  return createPortal(modalContent, document.body)
}
