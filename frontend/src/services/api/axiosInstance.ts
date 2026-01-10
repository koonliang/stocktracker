import axios from 'axios'

// Cookie utility functions for SSR compatibility
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

const axiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Only add interceptors on client side
if (typeof window !== 'undefined') {
  // Request interceptor
  axiosInstance.interceptors.request.use(
    config => {
      const token = getCookie('authToken')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    },
    error => {
      return Promise.reject(error)
    }
  )

  // Response interceptor
  axiosInstance.interceptors.response.use(
    response => response,
    error => {
      const status = error.response?.status
      const url = error.config?.url

      // Handle 401 Unauthorized - Invalid/expired token
      if (status === 401 && !url?.includes('/auth/login') && !url?.includes('/auth/logout')) {
        deleteCookie('authToken')
        deleteCookie('user')
        window.location.href = '/login'
        return Promise.reject(error)
      }

      // Handle 403 Forbidden - Insufficient permissions
      if (status === 403) {
        // Clear auth data and redirect to home page
        deleteCookie('authToken')
        deleteCookie('user')
        window.location.href = '/'
        return Promise.reject(error)
      }

      return Promise.reject(error)
    }
  )
}

export default axiosInstance
