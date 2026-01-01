import axios from 'axios'

const axiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor
axiosInstance.interceptors.request.use(
  config => {
    const token = localStorage.getItem('authToken')
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
      localStorage.removeItem('authToken')
      localStorage.removeItem('user')
      window.location.href = '/login'
      return Promise.reject(error)
    }

    // Handle 403 Forbidden - Insufficient permissions
    if (status === 403) {
      // Clear auth data and redirect to home page
      localStorage.removeItem('authToken')
      localStorage.removeItem('user')
      window.location.href = '/'
      return Promise.reject(error)
    }

    return Promise.reject(error)
  }
)

export default axiosInstance
