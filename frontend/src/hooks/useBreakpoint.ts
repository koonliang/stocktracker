import { useState, useEffect } from 'react'

type Breakpoint = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'

const breakpoints = {
  xs: 0,
  sm: 640,
  md: 768,
  lg: 1024,
  xl: 1280,
  '2xl': 1536,
}

export function useBreakpoint() {
  const [breakpoint, setBreakpoint] = useState<Breakpoint>(() => {
    if (typeof window === 'undefined') return 'md'
    const width = window.innerWidth
    if (width >= breakpoints['2xl']) return '2xl'
    if (width >= breakpoints.xl) return 'xl'
    if (width >= breakpoints.lg) return 'lg'
    if (width >= breakpoints.md) return 'md'
    if (width >= breakpoints.sm) return 'sm'
    return 'xs'
  })

  useEffect(() => {
    const handleResize = () => {
      const width = window.innerWidth
      if (width >= breakpoints['2xl']) setBreakpoint('2xl')
      else if (width >= breakpoints.xl) setBreakpoint('xl')
      else if (width >= breakpoints.lg) setBreakpoint('lg')
      else if (width >= breakpoints.md) setBreakpoint('md')
      else if (width >= breakpoints.sm) setBreakpoint('sm')
      else setBreakpoint('xs')
    }

    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  return {
    breakpoint,
    isMobile: breakpoint === 'xs' || breakpoint === 'sm',
    isTablet: breakpoint === 'md',
    isDesktop: breakpoint === 'lg' || breakpoint === 'xl' || breakpoint === '2xl',
    isAtLeast: (bp: Breakpoint) => breakpoints[breakpoint] >= breakpoints[bp],
    isAtMost: (bp: Breakpoint) => breakpoints[breakpoint] <= breakpoints[bp],
  }
}
