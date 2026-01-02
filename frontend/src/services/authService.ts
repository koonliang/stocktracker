import api from './api'
import type { LoginRequest, SignupRequest, AuthResponse, ApiResponse } from '@/types/auth'

const TOKEN_KEY = 'authToken'
const USER_KEY = 'user'

export const authService = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    try {
      const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', credentials)

      if (response.data.success && response.data.data) {
        const authData = response.data.data
        localStorage.setItem(TOKEN_KEY, authData.token)
        localStorage.setItem(
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
        localStorage.setItem(TOKEN_KEY, authData.token)
        localStorage.setItem(
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
      // ALWAYS clear local data regardless of backend response
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  },

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY)
  },

  getUser(): { id: number; email: string; name: string } | null {
    const user = localStorage.getItem(USER_KEY)
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
        localStorage.setItem(TOKEN_KEY, authData.token)
        localStorage.setItem(
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
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(
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
