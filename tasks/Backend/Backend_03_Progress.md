# Backend_03 Implementation Progress

## Implementation of Vercel Deployment Plan (Monorepo Architecture)

**Date Started:** January 11, 2026
**Status:** In Progress - Phase 1 Complete

---

## Completed Tasks

### Phase 0: Folder Restructuring ✅
- [x] Created new branch `refactor-monorepo`
- [x] Moved all frontend files from `frontend/` to root directory
  - Moved `src/`, `public/`, `package.json`, `next.config.ts`, `tsconfig.json`
  - Moved `tailwind.config.ts`, `eslint.config.js`, `.prettierrc`, `.prettierignore`
  - Moved `postcss.config.cjs`, `next-env.d.ts`
- [x] Moved `backend-nodejs/prisma/` to root `prisma/` directory
- [x] Updated `.gitignore` with merged patterns from both frontend and backend
- [x] Merged `package.json` dependencies from frontend and backend-nodejs
  - Added Prisma, bcrypt, passport, jsonwebtoken, class-validator, etc.
  - Added all @types packages for TypeScript
- [x] Updated `tsconfig.json` paths to include `@lib/*` alias
- [x] Created `src/lib/` directory structure for backend logic
- [x] Tested Next.js dev server - working correctly

### Phase 1: Backend Refactoring ✅

#### Database Layer
- [x] Updated Prisma schema for PostgreSQL compatibility
  - Changed `provider = "mysql"` → `provider = "postgresql"`
  - Added `directUrl = env("DIRECT_URL")` for Supabase
  - Changed `BIT(1)` → `Boolean` with defaults
  - Changed `@db.DateTime(6)` → `@db.Timestamp(6)`
  - Added `@default(now())` and `@updatedAt` decorators
  - Changed `onDelete: NoAction` → `onDelete: Cascade`
  - Added `binaryTargets = ["native", "rhel-openssl-1.0.x"]` for Vercel
- [x] Created `src/lib/database/prisma.ts` singleton service
- [x] Created `src/lib/database/types.ts` with type aliases

#### Common Utilities
- [x] Migrated `src/lib/common/utils/password-validator.ts`
- [x] Migrated `src/lib/common/exceptions/`
  - `bad-request.exception.ts`
  - `unauthorized.exception.ts`
  - `index.ts`

#### User Service
- [x] Migrated `src/lib/user/user.service.ts`
  - Converted from NestJS service to standalone class
  - Uses Prisma singleton instead of dependency injection
  - Exported singleton instance

#### Auth Services
- [x] Migrated auth DTOs to `src/lib/auth/dto/`
  - `auth-response.dto.ts`
  - `login.dto.ts` (converted from class-validator to interface)
  - `signup.dto.ts` (converted from class-validator to interface)
  - `index.ts`
- [x] Created `src/lib/auth/jwt.service.ts`
  - Replaced NestJS JwtService with jsonwebtoken library
  - Added proper TypeScript types (JwtPayload | string | null)
  - Handles JWT secret and expiration from environment variables
  - Exported singleton instance
- [x] Created `src/lib/auth/auth.service.ts`
  - Converted from NestJS service to standalone class
  - Uses singleton instances of userService and jwtService
  - Implements login, register, and logout methods
  - Exported singleton instance

#### Authentication Middleware
- [x] Created `src/lib/middleware/auth.ts`
  - `authenticateRequest()` - Verify JWT and fetch user
  - `getOptionalUser()` - Optional authentication
  - Proper error handling with UnauthorizedException

#### Next.js API Routes
- [x] Created `src/app/api/auth/login/route.ts` (POST)
  - Validates email and password
  - Returns AuthResponse with JWT token
  - Error handling for 400, 401, 500
- [x] Created `src/app/api/auth/register/route.ts` (POST)
  - Validates all required fields
  - Registers new user and auto-login
  - Returns AuthResponse with JWT token
- [x] Created `src/app/api/auth/logout/route.ts` (POST)
  - Stateless logout (client discards token)
  - Returns success message

### Phase 4: Frontend API Client ✅
- [x] Verified `src/services/api/axiosInstance.ts` already uses relative URLs
  - `baseURL: '/api'` is correct for monorepo
  - JWT token from cookies auto-attached
  - 401/403 redirects handled

### Phase 5: Vercel Deployment Configuration ✅
- [x] Created `vercel.json` configuration
  - Build command: `prisma generate && next build`
  - Framework: nextjs
  - Region: iad1 (US East)
  - Environment variables placeholders
- [x] Updated `package.json` scripts
  - `build`: `prisma generate && next build`
  - `postinstall`: `prisma generate`
- [x] Created `.env.local.template` with all required variables
  - Database URLs (Supabase pooler and direct)
  - JWT configuration
  - Google OAuth2
  - External APIs (Yahoo Finance)
  - Cron secret
  - Next.js API URL

### Bug Fixes
- [x] Fixed TypeScript/ESLint errors
  - Fixed `any` types in jwt.service.ts → `jwt.JwtPayload | string | null`
  - Removed unused `request` parameter in logout route
  - Fixed unused `userId` variable in auth middleware
  - Moved `next-env.d.ts` to root directory

---

## Pending Tasks

### Phase 1: Google OAuth Implementation (Optional)
- [ ] Create `src/lib/auth/strategies/google-oauth.service.ts`
- [ ] Create `src/app/api/auth/oauth2/callback/google/route.ts`
- [ ] Test Google OAuth flow locally

### Phase 2: Database Setup (Not Yet Started)
- [ ] Create Supabase account and project
- [ ] Get connection strings (pooler and direct URLs)
- [ ] Export data from current MySQL database
- [ ] Push Prisma schema to Supabase: `npx prisma db push`
- [ ] Import data to Supabase (CSV or SQL)
- [ ] Verify data migration
- [ ] Test local connection with Supabase DATABASE_URL

### Phase 3: Cron Jobs (Not Yet Started)
- [ ] Create `src/app/api/cron/cleanup-demo-accounts/route.ts`
- [ ] Add cron configuration to vercel.json
- [ ] Test cron endpoint with CRON_SECRET

### Additional API Routes (Not Yet Started)
- [ ] Create portfolios API routes
- [ ] Create transactions API routes
- [ ] Create holdings API routes
- [ ] Test all API routes with Postman/Thunder Client

---

## Next Steps

1. **Test Local Build**
   ```bash
   npm run build
   npm run start
   ```

2. **Database Migration to Supabase**
   - Sign up at supabase.com
   - Create new project
   - Get connection strings
   - Update `.env.local` with real database URLs
   - Run `npx prisma db push`

3. **Test Authentication Flow**
   - Test `/api/auth/register`
   - Test `/api/auth/login`
   - Test protected routes with JWT token
   - Test `/api/auth/logout`

4. **Deploy to Vercel Preview**
   - Install Vercel CLI: `npm i -g vercel`
   - Run `vercel` for preview deployment
   - Add environment variables in Vercel dashboard
   - Test deployed application

5. **Production Deployment**
   - Run `vercel --prod`
   - Update Google OAuth redirect URIs
   - Monitor Vercel function logs

---

## File Structure

```
stocktracker/ (root)
├── src/
│   ├── app/
│   │   ├── api/                    # Backend API routes (NEW)
│   │   │   └── auth/
│   │   │       ├── login/route.ts
│   │   │       ├── register/route.ts
│   │   │       └── logout/route.ts
│   │   ├── dashboard/              # Frontend pages
│   │   ├── login/
│   │   ├── register/
│   │   ├── page.tsx
│   │   └── layout.tsx
│   ├── components/                 # UI components
│   ├── services/api/               # Frontend API client
│   ├── lib/                        # Backend logic (NEW)
│   │   ├── auth/
│   │   │   ├── dto/
│   │   │   ├── auth.service.ts
│   │   │   └── jwt.service.ts
│   │   ├── database/
│   │   │   ├── prisma.ts
│   │   │   └── types.ts
│   │   ├── user/
│   │   │   └── user.service.ts
│   │   ├── common/
│   │   │   ├── utils/
│   │   │   └── exceptions/
│   │   └── middleware/
│   │       └── auth.ts
│   ├── hooks/
│   ├── types/
│   └── utils/
├── public/
├── prisma/
│   └── schema.prisma
├── package.json
├── next.config.ts
├── tsconfig.json
├── vercel.json
├── .env.local.template
└── .gitignore
```

---

## Architecture Decisions

### Why Monorepo?
- Single deployment to Vercel
- No CORS issues (same origin)
- Code sharing between frontend/backend
- Next.js automatic API route optimization

### Why Supabase PostgreSQL?
- Serverless-native with pgBouncer connection pooling
- No connection limit issues with Vercel functions
- Free tier: 500 MB database, 2 GB bandwidth
- More powerful than MySQL (JSON support, CTEs, etc.)

### Why Next.js API Routes Instead of NestJS?
- Serverless functions compatible with Vercel
- No need to maintain separate backend server
- Automatic optimization and edge function support
- Simpler deployment (one codebase, one domain)

### Authentication Approach
- JWT tokens (stateless, no session management needed)
- Tokens stored in cookies on client
- Each API route verifies JWT independently
- User re-fetched from database on each request (ensures enabled status)

---

## Known Issues / Warnings

1. **5099 ESLint Warnings**
   - Most are unused variables/imports from moved files
   - Can be cleaned up with `npm run lint:fix`
   - Non-blocking for build

2. **Frontend Directory**
   - Old `frontend/` directory still exists with duplicates
   - Can be deleted after verification
   - All files successfully moved to root

3. **backend-nodejs Still Referenced**
   - Old backend code still present
   - Will be archived after full migration
   - Prisma schema moved but source code remains

4. **Database Not Set Up Yet**
   - `.env.local` needs to be created from template
   - Supabase project needs to be created
   - Data migration pending

---

## Testing Checklist

### Local Testing
- [ ] `npm install` - installs all dependencies
- [ ] `npm run build` - builds Next.js app
- [ ] `npm run dev` - starts dev server
- [ ] Navigate to http://localhost:3000
- [ ] Test register page UI
- [ ] Test login page UI
- [ ] Test dashboard (should redirect if not authenticated)

### API Testing (Once Database is Set Up)
- [ ] POST /api/auth/register - Create new user
- [ ] POST /api/auth/login - Login with credentials
- [ ] POST /api/auth/logout - Logout
- [ ] Verify JWT token in response
- [ ] Test invalid credentials (401 error)
- [ ] Test duplicate email (400 error)

### Vercel Preview Testing
- [ ] Deploy to preview: `vercel`
- [ ] Test all routes work on Vercel
- [ ] Check Vercel function logs
- [ ] Verify database connection from Vercel
- [ ] Test cold start latency

---

## Resources

- Vercel Documentation: https://vercel.com/docs
- Supabase Documentation: https://supabase.com/docs
- Prisma Documentation: https://www.prisma.io/docs
- Next.js API Routes: https://nextjs.org/docs/app/building-your-application/routing/route-handlers
- jsonwebtoken: https://github.com/auth0/node-jsonwebtoken
