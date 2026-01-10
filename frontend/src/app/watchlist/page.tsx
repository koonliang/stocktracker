'use client'

import { DashboardNavigation } from '@components/layout'

export default function Watchlist() {
  return (
    <div className="min-h-screen bg-background">
      <DashboardNavigation />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8 sm:py-12">
        <header className="mb-8">
          <h1 className="text-3xl sm:text-4xl font-bold text-slate-900 mb-2">Watchlist</h1>
          <p className="text-slate-600">Track stocks you&apos;re interested in</p>
        </header>

        <main>
          <div className="bg-white rounded-xl border border-slate-100 shadow-soft p-12 text-center">
            <p className="text-slate-500">Watchlist page coming soon...</p>
          </div>
        </main>
      </div>
    </div>
  )
}
