import api from './api'
import type { LoginRequest, SignupRequest, AuthResponse, ApiResponse } from '@/types/auth'

const TOKEN_KEY = 'authToken'
const USER_KEY = 'user'

// Cookie utility functions for SSR compatibility
const setCookie = (name: string, value: string, days: number = 7) => {
  if (typeof window !== 'undefined') {
    const expires = new Date(Date.now() + days * 864e5).toUTCString()
    document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Lax`
  }
}

const getCookie = (name: string): string | null => {
  if (typeof window === 'undefined') return null
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`)
  if (parts.length === 2) {
    return decodeURIComponent(parts.pop()?.split(';').shift() || '')
  }
  return null
}

const deleteCookie = (name: string) => {
  if (typeof window !== 'undefined') {
    document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`
  }
}

export const authService = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    try {
      const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', credentials)

      if (response.data.success && response.data.data) {
        const authData = response.data.data
        setCookie(TOKEN_KEY, authData.token)
        setCookie(
          USER_KEY,
          JSON.stringify({
            id: authData.userId,
            email: authData.email,
            name: authData.name,
          })
        )
        return authData
      }

      throw new Error(response.data.message || 'Login failed')
    } catch (error: unknown) {
      // Extract error message from backend response
      const message =
        (error as { response?: { data?: { message?: string } }; message?: string }).response?.data
          ?.message ||
        (error as { message?: string }).message ||
        'Login failed'
      throw new Error(message)
    }
  },

  async demoLogin(): Promise<AuthResponse> {
    try {
      const response = await api.post<ApiResponse<AuthResponse>>('/auth/demo-login')

      if (response.data.success && response.data.data) {
        const authData = response.data.data
        setCookie(TOKEN_KEY, authData.token)
        setCookie(
          USER_KEY,
          JSON.stringify({
            id: authData.userId,
            email: authData.email,
            name: authData.name,
          })
        )
        return authData
      }

      throw new Error(response.data.message || 'Demo login failed')
    } catch (error: unknown) {
      const message =
        (error as { response?: { data?: { message?: string } }; message?: string }).response?.data
          ?.message ||
        (error as { message?: string }).message ||
        'Demo login failed'
      throw new Error(message)
    }
  },

  async logout(): Promise<void> {
    try {
      // Call backend logout endpoint (if implemented)
      await api.post('/auth/logout', {}, { timeout: 3000 })
    } catch (error) {
      // Log error for debugging but don't block logout
      console.warn('Backend logout failed, proceeding with local cleanup:', error)
    } finally {
      // ALWAYS clear cookies regardless of backend response
      deleteCookie(TOKEN_KEY)
      deleteCookie(USER_KEY)
    }
  },

  getToken(): string | null {
    return getCookie(TOKEN_KEY)
  },

  getUser(): { id: number; email: string; name: string } | null {
    const user = getCookie(USER_KEY)
    return user ? JSON.parse(user) : null
  },

  isAuthenticated(): boolean {
    return !!this.getToken()
  },

  async register(data: SignupRequest): Promise<AuthResponse> {
    try {
      const response = await api.post<ApiResponse<AuthResponse>>('/auth/register', data)

      if (response.data.success && response.data.data) {
        const authData = response.data.data
        setCookie(TOKEN_KEY, authData.token)
        setCookie(
          USER_KEY,
          JSON.stringify({
            id: authData.userId,
            email: authData.email,
            name: authData.name,
          })
        )
        return authData
      }

      throw new Error(response.data.message || 'Registration failed')
    } catch (error: unknown) {
      const message =
        (error as { response?: { data?: { message?: string } }; message?: string }).response?.data
          ?.message ||
        (error as { message?: string }).message ||
        'Registration failed'
      throw new Error(message)
    }
  },

  storeOAuthCredentials(data: {
    token: string
    userId: number
    email: string
    name: string
  }): void {
    setCookie(TOKEN_KEY, data.token)
    setCookie(
      USER_KEY,
      JSON.stringify({
        id: data.userId,
        email: data.email,
        name: data.name,
      })
    )
  },
}

export default authService
