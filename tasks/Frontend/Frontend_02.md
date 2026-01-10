# Frontend_02: Migrate Frontend to Next.js

## Objective
Change the frontend tech stack to continue to use React, but use Next.js for server-side rendering.

---

## Current State Analysis

### Tech Stack Being Replaced
- **Build Tool**: Vite 7.2.4
- **Routing**: React Router DOM 7.11.0
- **React Version**: 19.2.0
- **TypeScript**: 5.9.3
- **Styling**: Tailwind CSS 3.4.19 + CSS Modules

### Current Route Structure
| Route | Component | Protected |
|-------|-----------|-----------|
| `/` | Home | No |
| `/login` | Login | No |
| `/register` | Register | No |
| `/oauth2/redirect` | OAuth2Redirect | No |
| `/logout` | Logout | No |
| `/dashboard` | Dashboard | Yes |
| `/dashboard/transactions` | Transactions | Yes |
| `/portfolios` | Portfolios | Yes |
| `/watchlist` | Watchlist | Yes |

### Components to Migrate (25 total)
- 9 Pages
- 16 Reusable components (dashboard, transactions, import, layout, auth, common)

### Dependencies to Retain
- axios (HTTP client)
- recharts (charting)
- papaparse (CSV parsing)
- @tailwindcss/forms

---

## Implementation Details

### Phase 1: Project Setup

#### 1.1 Create New Next.js Project Structure
```bash
# From project root
cd frontend
npx create-next-app@latest . --typescript --tailwind --eslint --app --src-dir --import-alias "@/*"
```

#### 1.2 New package.json Configuration
```json
{
  "name": "stocktracker-frontend",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "dev": "next dev -p 3000",
    "build": "next build",
    "start": "next start",
    "lint": "next lint",
    "lint:fix": "next lint --fix",
    "format": "prettier --write \"src/**/*.{ts,tsx,css,json}\""
  },
  "dependencies": {
    "axios": "^1.13.2",
    "next": "^15.3.0",
    "papaparse": "^5.5.3",
    "react": "^19.2.0",
    "react-dom": "^19.2.0",
    "recharts": "^3.6.0"
  },
  "devDependencies": {
    "@tailwindcss/forms": "^0.5.11",
    "@types/node": "^24.10.4",
    "@types/papaparse": "^5.5.2",
    "@types/react": "^19.2.5",
    "@types/react-dom": "^19.2.3",
    "autoprefixer": "^10.4.23",
    "eslint": "^9.39.1",
    "eslint-config-next": "^15.3.0",
    "postcss": "^8.5.6",
    "prettier": "^3.7.4",
    "tailwindcss": "^3.4.19",
    "typescript": "~5.9.3"
  }
}
```

#### 1.3 Files to Remove
- `vite.config.ts`
- `index.html`
- `src/main.tsx`
- `src/App.tsx`
- `@vitejs/plugin-react` dependency
- `react-router-dom` dependency

---

### Phase 2: Next.js App Router Structure

#### 2.1 New Directory Structure
```
frontend/
├── src/
│   ├── app/                          # Next.js App Router
│   │   ├── layout.tsx                # Root layout
│   │   ├── page.tsx                  # Home page (/)
│   │   ├── login/
│   │   │   └── page.tsx              # /login
│   │   ├── register/
│   │   │   └── page.tsx              # /register
│   │   ├── logout/
│   │   │   └── page.tsx              # /logout
│   │   ├── oauth2/
│   │   │   └── redirect/
│   │   │       └── page.tsx          # /oauth2/redirect
│   │   ├── dashboard/
│   │   │   ├── layout.tsx            # Protected layout wrapper
│   │   │   ├── page.tsx              # /dashboard
│   │   │   └── transactions/
│   │   │       └── page.tsx          # /dashboard/transactions
│   │   ├── portfolios/
│   │   │   └── page.tsx              # /portfolios (protected)
│   │   └── watchlist/
│   │       └── page.tsx              # /watchlist (protected)
│   ├── components/                   # Keep existing structure
│   ├── hooks/                        # Keep existing hooks
│   ├── services/                     # Keep existing API services
│   ├── types/                        # Keep existing types
│   ├── utils/                        # Keep existing utilities
│   └── middleware.ts                 # Auth middleware
├── public/                           # Static assets
├── next.config.ts                    # Next.js configuration
├── tailwind.config.ts                # Tailwind (convert from .cjs)
├── postcss.config.js                 # PostCSS config
└── tsconfig.json                     # TypeScript config
```

#### 2.2 Root Layout (`src/app/layout.tsx`)
```tsx
import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'Stock Tracker',
  description: 'Track your stock portfolio performance',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <head>
        <link
          href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap"
          rel="stylesheet"
        />
      </head>
      <body className="min-h-screen bg-background">
        {children}
      </body>
    </html>
  )
}
```

---

### Phase 3: Authentication Migration

#### 3.1 Next.js Middleware (`src/middleware.ts`)
```tsx
import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

const protectedRoutes = ['/dashboard', '/portfolios', '/watchlist']
const authRoutes = ['/login', '/register']

export function middleware(request: NextRequest) {
  const token = request.cookies.get('authToken')?.value
  const { pathname } = request.nextUrl

  // Check if route is protected
  const isProtectedRoute = protectedRoutes.some(route =>
    pathname.startsWith(route)
  )
  const isAuthRoute = authRoutes.includes(pathname)

  // Redirect to login if accessing protected route without token
  if (isProtectedRoute && !token) {
    const loginUrl = new URL('/login', request.url)
    loginUrl.searchParams.set('from', pathname)
    return NextResponse.redirect(loginUrl)
  }

  // Redirect to dashboard if accessing auth routes while logged in
  if (isAuthRoute && token) {
    return NextResponse.redirect(new URL('/dashboard', request.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: [
    '/dashboard/:path*',
    '/portfolios/:path*',
    '/watchlist/:path*',
    '/login',
    '/register',
  ],
}
```

#### 3.2 Update Auth Service for Cookie-Based Tokens
Modify `src/services/authService.ts` to use cookies instead of localStorage for SSR compatibility:

```tsx
// Add cookie utility functions
const setCookie = (name: string, value: string, days: number = 7) => {
  if (typeof window !== 'undefined') {
    const expires = new Date(Date.now() + days * 864e5).toUTCString()
    document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Lax`
  }
}

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
```

---

### Phase 4: API Configuration

#### 4.1 Next.js Config (`next.config.ts`)
```typescript
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
```

#### 4.2 Update Axios Instance
Modify `src/services/api/axiosInstance.ts` for SSR compatibility:

```tsx
import axios from 'axios'

const axiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Only add interceptors on client side
if (typeof window !== 'undefined') {
  axiosInstance.interceptors.request.use(
    (config) => {
      const token = getCookie('authToken')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    },
    (error) => Promise.reject(error)
  )

  axiosInstance.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401 || error.response?.status === 403) {
        deleteCookie('authToken')
        deleteCookie('user')
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
  )
}

export default axiosInstance
```

---

### Phase 5: Page Migration

#### 5.1 Page Migration Strategy
Each page follows this pattern:

**For Client Components (most pages):**
```tsx
'use client'

// Import existing component
import { OriginalPageComponent } from '@/components/...'
// Or inline the existing page code

export default function PageName() {
  return <OriginalPageComponent />
}
```

**For pages that can be Server Components:**
- Home page (static content)

#### 5.2 Specific Page Migrations

| Current File | New Location | Type |
|--------------|--------------|------|
| `pages/Home/Home.tsx` | `app/page.tsx` | Server Component |
| `pages/Login/Login.tsx` | `app/login/page.tsx` | Client Component |
| `pages/Register/Register.tsx` | `app/register/page.tsx` | Client Component |
| `pages/Logout/Logout.tsx` | `app/logout/page.tsx` | Client Component |
| `pages/OAuth2Redirect/OAuth2Redirect.tsx` | `app/oauth2/redirect/page.tsx` | Client Component |
| `pages/Dashboard/Dashboard.tsx` | `app/dashboard/page.tsx` | Client Component |
| `pages/Transactions/Transactions.tsx` | `app/dashboard/transactions/page.tsx` | Client Component |
| `pages/Portfolios/Portfolios.tsx` | `app/portfolios/page.tsx` | Client Component |
| `pages/Watchlist/Watchlist.tsx` | `app/watchlist/page.tsx` | Client Component |

#### 5.3 CSS Module Migration
CSS Modules work identically in Next.js. No changes needed for:
- `Login.module.css`
- `Register.module.css`
- `OAuth2Redirect.module.css`
- `PasswordStrengthIndicator.module.css`

---

### Phase 6: TypeScript Configuration

#### 6.1 Updated tsconfig.json
```json
{
  "compilerOptions": {
    "target": "ES2017",
    "lib": ["dom", "dom.iterable", "esnext"],
    "allowJs": true,
    "skipLibCheck": true,
    "strict": true,
    "noEmit": true,
    "esModuleInterop": true,
    "module": "esnext",
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "preserve",
    "incremental": true,
    "plugins": [
      {
        "name": "next"
      }
    ],
    "paths": {
      "@/*": ["./src/*"],
      "@components/*": ["./src/components/*"],
      "@pages/*": ["./src/app/*"],
      "@services/*": ["./src/services/*"],
      "@hooks/*": ["./src/hooks/*"],
      "@utils/*": ["./src/utils/*"],
      "@types/*": ["./src/types/*"],
      "@assets/*": ["./src/assets/*"]
    }
  },
  "include": ["next-env.d.ts", "**/*.ts", "**/*.tsx", ".next/types/**/*.ts"],
  "exclude": ["node_modules"]
}
```

---

### Phase 7: Styling Migration

#### 7.1 Convert Tailwind Config
Rename `tailwind.config.cjs` to `tailwind.config.ts`:

```typescript
import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        background: '#F8FAFC',
        foreground: '#FFFFFF',
        primary: '#4F46E5',
        secondary: '#7C3AED',
        accent: '#10B981',
        border: '#E2E8F0',
      },
      fontFamily: {
        sans: ['Plus Jakarta Sans', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        soft: '0 2px 15px -3px rgba(0, 0, 0, 0.07), 0 10px 20px -2px rgba(0, 0, 0, 0.04)',
        'soft-hover': '0 4px 20px -3px rgba(0, 0, 0, 0.1), 0 12px 25px -2px rgba(0, 0, 0, 0.06)',
        button: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)',
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        fadeIn: 'fadeIn 0.2s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(-10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [require('@tailwindcss/forms')],
}

export default config
```

#### 7.2 Global Styles (`src/app/globals.css`)
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

/* Import existing global styles if any */
```

---

### Phase 8: Environment Variables

#### 8.1 Environment File Convention
Rename `.env` to `.env.local` for Next.js:

```env
# Public variables (accessible in browser)
NEXT_PUBLIC_API_URL=http://localhost:8080

# Server-only variables
BACKEND_URL=http://localhost:8080
```

#### 8.2 Usage in Code
```tsx
// Client-side: use NEXT_PUBLIC_ prefix
const apiUrl = process.env.NEXT_PUBLIC_API_URL

// Server-side only
const backendUrl = process.env.BACKEND_URL
```

---

## Migration Checklist

### Pre-Migration
- [ ] Backup current frontend directory
- [ ] Document all current routes and their behavior
- [ ] List all environment variables in use

### Phase 1: Setup
- [ ] Install Next.js dependencies
- [ ] Remove Vite-specific files and dependencies
- [ ] Create Next.js config files

### Phase 2: Structure
- [ ] Create app directory structure
- [ ] Set up root layout
- [ ] Create page files for each route

### Phase 3: Authentication
- [ ] Create middleware.ts
- [ ] Update authService for cookie-based auth
- [ ] Test protected route redirects

### Phase 4: API
- [ ] Configure rewrites in next.config.ts
- [ ] Update axios instance for SSR
- [ ] Test API calls work correctly

### Phase 5: Pages
- [ ] Migrate Home page
- [ ] Migrate Login page
- [ ] Migrate Register page
- [ ] Migrate OAuth2Redirect page
- [ ] Migrate Logout page
- [ ] Migrate Dashboard page
- [ ] Migrate Transactions page
- [ ] Migrate Portfolios page
- [ ] Migrate Watchlist page

### Phase 6: Components
- [ ] Verify all components work as Client Components
- [ ] Fix any SSR-related issues (useEffect, window access, etc.)
- [ ] Ensure CSS Modules load correctly

### Phase 7: Testing
- [ ] Run `npm run dev` and verify all routes
- [ ] Test authentication flow (login/logout)
- [ ] Test OAuth2 flow
- [ ] Test protected routes
- [ ] Test API calls
- [ ] Verify styling renders correctly
- [ ] Test production build (`npm run build && npm start`)

### Phase 8: Cleanup
- [ ] Remove unused files from old structure
- [ ] Update any documentation
- [ ] Verify build scripts work correctly

---

## Verification Steps

1. **Development Server**
   ```bash
   cd frontend
   npm run dev
   ```
   - Verify home page loads at http://localhost:3000
   - Verify login/register pages work
   - Verify OAuth2 redirect works
   - Verify dashboard loads with authentication

2. **Production Build**
   ```bash
   npm run build
   npm start
   ```
   - Verify no build errors
   - Test all routes in production mode

3. **Backend Integration**
   - Start backend on port 8080
   - Test API calls through Next.js rewrites
   - Verify CORS and proxy configuration

---

## Rollback Plan

If migration fails:
1. Keep original `frontend-backup` directory
2. Restore from backup: `rm -rf frontend && mv frontend-backup frontend`
3. Run `npm install` to restore node_modules

---

## Notes

- Most existing code (hooks, services, components) requires minimal changes
- Main changes are in routing and authentication middleware
- Tailwind CSS and CSS Modules work identically in Next.js
- Consider future SSR optimizations for Dashboard data fetching
