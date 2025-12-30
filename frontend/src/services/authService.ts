import api from './api'
import type { LoginRequest, AuthResponse, ApiResponse } from '@/types/auth'

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

  logout(): void {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
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
}

export default authService
