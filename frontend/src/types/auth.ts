export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  type: string
  userId: number
  email: string
  name: string
}

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
}

export interface User {
  id: number
  email: string
  name: string
}
