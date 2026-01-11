'use client'

import { useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { authService } from '@/services/authService'
import styles from './page.module.css'

export default function OAuth2Redirect() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [error, setError] = useState<string | null>(null)
  const [isProcessing, setIsProcessing] = useState(true)

  useEffect(() => {
    const processOAuthCallback = () => {
      const token = searchParams.get('token')
      const errorParam = searchParams.get('error')
      const userId = searchParams.get('userId')
      const email = searchParams.get('email')
      const name = searchParams.get('name')

      if (errorParam) {
        setError(decodeURIComponent(errorParam))
        setIsProcessing(false)
        return
      }

      if (token && userId && email && name) {
        // Store auth data from OAuth callback
        authService.storeOAuthCredentials({
          token,
          userId: parseInt(userId, 10),
          email,
          name,
        })
        router.replace('/dashboard')
      } else {
        setError('Invalid OAuth response')
        setIsProcessing(false)
      }
    }

    processOAuthCallback()
  }, [searchParams, router])

  if (error) {
    return (
      <div className={styles.container}>
        <div className={styles.card}>
          <h1 className={styles.title}>Authentication Failed</h1>
          <p className={styles.error}>{error}</p>
          <button className={styles.button} onClick={() => router.push('/login')}>
            Back to Login
          </button>
        </div>
      </div>
    )
  }

  if (isProcessing) {
    return (
      <div className={styles.container}>
        <div className={styles.card}>
          <div className={styles.spinner} />
          <p className={styles.text}>Completing sign in...</p>
        </div>
      </div>
    )
  }

  return null
}
