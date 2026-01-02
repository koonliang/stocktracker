import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { authService } from '@/services/authService'
import styles from './OAuth2Redirect.module.css'

const OAuth2Redirect = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const token = searchParams.get('token')
    const errorParam = searchParams.get('error')
    const userId = searchParams.get('userId')
    const email = searchParams.get('email')
    const name = searchParams.get('name')

    if (errorParam) {
      setError(decodeURIComponent(errorParam))
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
      navigate('/dashboard', { replace: true })
    } else {
      setError('Invalid OAuth response')
    }
  }, [searchParams, navigate])

  if (error) {
    return (
      <div className={styles.container}>
        <div className={styles.card}>
          <h1 className={styles.title}>Authentication Failed</h1>
          <p className={styles.error}>{error}</p>
          <button
            className={styles.button}
            onClick={() => navigate('/login')}
          >
            Back to Login
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.spinner} />
        <p className={styles.text}>Completing sign in...</p>
      </div>
    </div>
  )
}

export default OAuth2Redirect
