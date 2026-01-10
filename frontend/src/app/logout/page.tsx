'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'

export default function Logout() {
  const router = useRouter()
  const [countdown, setCountdown] = useState(3)

  useEffect(() => {
    // Countdown timer
    const timer = setInterval(() => {
      setCountdown(prev => {
        if (prev <= 1) {
          clearInterval(timer)
          router.push('/login')
          return 0
        }
        return prev - 1
      })
    }, 1000)

    return () => clearInterval(timer)
  }, [router])

  const handleLoginClick = () => {
    router.push('/login')
  }

  const handleHomeClick = () => {
    router.push('/')
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-4">
      <div className="max-w-md w-full bg-white border border-slate-200 rounded-lg shadow-sm p-8">
        {/* Success Icon */}
        <div className="flex justify-center mb-6">
          <div className="w-16 h-16 bg-emerald-100 rounded-full flex items-center justify-center">
            <svg
              className="w-10 h-10 text-emerald-500"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M5 13l4 4L19 7"
              />
            </svg>
          </div>
        </div>

        {/* Heading */}
        <h1 className="text-2xl font-bold text-slate-900 text-center mb-3">
          You&apos;ve been logged out
        </h1>

        {/* Message */}
        <p className="text-sm text-slate-600 text-center mb-6">
          Your session has ended securely.
          <br />
          Redirecting to login in {countdown}s...
        </p>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-3">
          <button
            onClick={handleLoginClick}
            className="flex-1 h-11 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors duration-200"
          >
            Return to Login
          </button>
          <button
            onClick={handleHomeClick}
            className="flex-1 h-11 bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium rounded-lg transition-colors duration-200"
          >
            Go to Home
          </button>
        </div>
      </div>
    </div>
  )
}
