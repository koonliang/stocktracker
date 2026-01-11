'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { Navigation } from '@components/layout'
import { authService } from '@/services/authService'

export default function Home() {
  const [isLoadingDemo, setIsLoadingDemo] = useState(false)
  const router = useRouter()

  const handleDemoLogin = async () => {
    try {
      setIsLoadingDemo(true)

      // If user is already logged in, just navigate to dashboard
      if (authService.isAuthenticated()) {
        router.push('/dashboard')
        return
      }

      // Otherwise, create a new demo account
      await authService.demoLogin()
      router.push('/dashboard')
    } catch (error) {
      console.error('Demo login failed:', error)
      alert('Failed to create demo account. Please try again.')
    } finally {
      setIsLoadingDemo(false)
    }
  }

  return (
    <div className="min-h-screen bg-background">
      <Navigation />

      {/* Hero Section */}
      <section className="relative overflow-hidden">
        {/* Background gradient blobs */}
        <div className="absolute inset-0 overflow-hidden pointer-events-none">
          <div className="absolute top-20 left-10 w-96 h-96 bg-gradient-to-br from-indigo-100 to-violet-100 rounded-full blur-3xl opacity-50" />
          <div className="absolute bottom-20 right-10 w-96 h-96 bg-gradient-to-br from-violet-100 to-indigo-100 rounded-full blur-3xl opacity-50" />
        </div>

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 py-16 sm:py-20 lg:py-24">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            {/* Left Column - Text Content */}
            <div className="space-y-8">
              {/* Badge */}
              <div className="inline-flex items-center gap-2 px-3 py-1 bg-white border border-slate-200 rounded-full shadow-soft">
                <span className="px-2 py-0.5 bg-indigo-600 text-white text-xs font-bold rounded-full">
                  NEW
                </span>
                <span className="text-sm text-slate-600">v2.0 is now live</span>
              </div>

              {/* Hero Headline */}
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold leading-tight tracking-tight">
                Transform the way{' '}
                <span className="bg-gradient-to-r from-indigo-600 to-violet-600 bg-clip-text text-transparent">
                  you track stocks
                </span>
              </h1>

              {/* Hero Subtext */}
              <p className="text-lg text-slate-600 max-w-xl leading-relaxed">
                Stock Tracker brings your portfolio together with powerful tools designed to
                streamline workflows, boost productivity, and drive results.
              </p>

              {/* CTA Buttons */}
              <div className="flex flex-col sm:flex-row gap-4">
                <Link
                  href="/login"
                  className="inline-flex items-center justify-center gap-2 px-6 py-3 bg-gradient-to-r from-indigo-600 to-violet-600 text-white font-semibold rounded-full shadow-button hover:-translate-y-0.5 transition-all duration-200"
                >
                  Start free trial
                  <svg
                    className="w-4 h-4 transition-transform group-hover:translate-x-1"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M9 5l7 7-7 7"
                    />
                  </svg>
                </Link>
                <button
                  onClick={handleDemoLogin}
                  disabled={isLoadingDemo}
                  className="inline-flex items-center justify-center gap-2 px-6 py-3 bg-white text-slate-700 font-medium rounded-full border border-slate-200 hover:bg-slate-50 hover:border-slate-300 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isLoadingDemo ? (
                    <>
                      <svg
                        className="w-5 h-5 animate-spin"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                        />
                      </svg>
                      Creating demo...
                    </>
                  ) : (
                    <>
                      <svg
                        className="w-5 h-5"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"
                        />
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                        />
                      </svg>
                      View demo
                    </>
                  )}
                </button>
              </div>

              {/* Trust Badge */}
              <p className="text-sm text-slate-500">No credit card required · 14-day free trial</p>
            </div>

            {/* Right Column - Visual Element */}
            <div className="relative hidden lg:block">
              <div className="perspective-[2000px]">
                <div className="relative bg-white rounded-2xl shadow-soft-hover border border-slate-100 p-8 rotate-y-[-8deg] rotate-x-[5deg] transition-transform hover:rotate-y-[-4deg] hover:rotate-x-[2deg] duration-500">
                  {/* Mock Dashboard */}
                  <div className="space-y-4">
                    {/* Header */}
                    <div className="flex items-center gap-2">
                      <div className="w-3 h-3 rounded-full bg-red-400" />
                      <div className="w-3 h-3 rounded-full bg-yellow-400" />
                      <div className="w-3 h-3 rounded-full bg-green-400" />
                    </div>

                    {/* Chart Placeholder */}
                    <div className="h-48 bg-gradient-to-br from-indigo-50 to-violet-50 rounded-lg" />

                    {/* Stats Cards */}
                    <div className="grid grid-cols-2 gap-4">
                      <div className="bg-gradient-to-br from-indigo-50 to-white p-4 rounded-lg">
                        <div className="h-16 bg-white/50 rounded" />
                      </div>
                      <div className="bg-gradient-to-br from-violet-50 to-white p-4 rounded-lg">
                        <div className="h-16 bg-white/50 rounded" />
                      </div>
                    </div>

                    {/* Status Badge */}
                    <div className="absolute -bottom-4 -right-4 bg-white rounded-xl shadow-soft-hover p-4 border border-slate-100">
                      <div className="flex items-center gap-2">
                        <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse" />
                        <div>
                          <p className="text-xs text-slate-500">Status</p>
                          <p className="text-sm font-semibold text-slate-900">System Optimal</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="bg-white border-t border-slate-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-16">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            <div className="text-center">
              <div className="text-3xl md:text-4xl font-bold text-slate-900">500k+</div>
              <div className="text-sm text-slate-500 mt-2">Active Users</div>
            </div>
            <div className="text-center">
              <div className="text-3xl md:text-4xl font-bold text-slate-900">99.99%</div>
              <div className="text-sm text-slate-500 mt-2">Uptime SLA</div>
            </div>
            <div className="text-center">
              <div className="text-3xl md:text-4xl font-bold text-slate-900">24/7</div>
              <div className="text-sm text-slate-500 mt-2">Support Access</div>
            </div>
            <div className="text-center">
              <div className="text-3xl md:text-4xl font-bold text-slate-900">$10M+</div>
              <div className="text-sm text-slate-500 mt-2">Customer Savings</div>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
