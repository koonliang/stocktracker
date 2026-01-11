'use client'

import { useState, type FormEvent } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import Link from 'next/link'
import { authService } from '@services/authService'
import styles from './page.module.css'

export default function Login() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  // Get the page user tried to access before being redirected to login
  const from = searchParams.get('from') || '/dashboard'

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')

    // Basic validation
    if (!email || !password) {
      setError('Please enter both email and password')
      return
    }

    if (!email.includes('@')) {
      setError('Please enter a valid email address')
      return
    }

    setIsLoading(true)

    try {
      await authService.login({ email, password })
      router.replace(from) // Redirect to original destination
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message)
      } else {
        setError('An unexpected error occurred. Please try again.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>Stock Tracker</h1>
        <p className={styles.subtitle}>Sign in to your account</p>

        <form onSubmit={handleSubmit} className={styles.form}>
          {error && <div className={styles.error}>{error}</div>}

          <div className={styles.field}>
            <label htmlFor="email" className={styles.label}>
              Email
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className={styles.input}
              placeholder="Enter your email"
              disabled={isLoading}
              autoComplete="email"
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="password" className={styles.label}>
              Password
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              className={styles.input}
              placeholder="Enter your password"
              disabled={isLoading}
              autoComplete="current-password"
            />
          </div>

          <button type="submit" className={styles.button} disabled={isLoading}>
            {isLoading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        {/* Divider */}
        <div className={styles.divider}>
          <span>or</span>
        </div>

        {/* Google OAuth Button */}
        <button
          type="button"
          className={styles.googleButton}
          onClick={() => {
            window.location.href = '/oauth2/authorize/google'
          }}
          disabled={isLoading}
        >
          <GoogleIcon />
          Continue with Google
        </button>

        {/* Link to registration */}
        <p className={styles.footer}>
          Don&apos;t have an account?{' '}
          <Link href="/register" className={styles.link}>
            Sign up
          </Link>
        </p>
      </div>
    </div>
  )
}

// Google Icon SVG component
const GoogleIcon = () => (
  <svg className={styles.googleIcon} viewBox="0 0 24 24">
    <path
      fill="#4285F4"
      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
    />
    <path
      fill="#34A853"
      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
    />
    <path
      fill="#FBBC05"
      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
    />
    <path
      fill="#EA4335"
      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
    />
  </svg>
)
