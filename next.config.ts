import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
      {
        source: '/oauth2/authorize/:path*',
        destination: 'http://localhost:8080/oauth2/authorize/:path*',
      },
    ]
  },
  // Enable React Strict Mode
  reactStrictMode: true,
  // Disable x-powered-by header
  poweredByHeader: false,
}

export default nextConfig
