import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

const protectedRoutes = ['/dashboard', '/portfolios', '/watchlist']
const authRoutes = ['/login', '/register']

export function middleware(request: NextRequest) {
  const token = request.cookies.get('authToken')?.value
  const { pathname } = request.nextUrl

  // Check if route is protected
  const isProtectedRoute = protectedRoutes.some(route =>
    pathname.startsWith(route)
  )
  const isAuthRoute = authRoutes.includes(pathname)

  // Redirect to login if accessing protected route without token
  if (isProtectedRoute && !token) {
    const loginUrl = new URL('/login', request.url)
    loginUrl.searchParams.set('from', pathname)
    return NextResponse.redirect(loginUrl)
  }

  // Redirect to dashboard if accessing auth routes while logged in
  if (isAuthRoute && token) {
    return NextResponse.redirect(new URL('/dashboard', request.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: [
    '/dashboard/:path*',
    '/portfolios/:path*',
    '/watchlist/:path*',
    '/login',
    '/register',
  ],
}
