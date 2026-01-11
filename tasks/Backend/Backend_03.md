# Vercel Deployment Plan: Stock Tracker (Next.js + Node.js Backend)

## Executive Summary

Migrate Stock Tracker to Vercel using a **monorepo architecture** with Next.js API routes hosting the NestJS backend logic, and migrating from MySQL (self-hosted) to **Supabase PostgreSQL**. Frontend will be refactored to root folder for a cleaner structure.

---

## Understanding Vercel Serverless Limitations

### Critical Constraints

1. **Function Timeout**
   - **Hobby Plan**: 10 seconds maximum execution time
   - **Pro Plan**: 60 seconds maximum (for Serverless Functions)
   - Your current backend operations should complete well within this

2. **Cold Starts**
   - Functions "sleep" after inactivity (typically 5 minutes)
   - First request after sleep: 1-3 second delay
   - Subsequent requests: Fast (warm instances)
   - Impact: Initial API calls may feel sluggish

3. **Stateless Functions**
   - Each function invocation is isolated
   - Cannot maintain in-memory state between requests
   - No background jobs, scheduled tasks, or WebSocket connections
   - Your current scheduler (`DemoAccountCleanupScheduler`) won't work

4. **Database Connections**
   - Traditional connection pools don't work well
   - Each function creates new DB connections
   - Risk: Exhausting database connection limits
   - Solution: Use connection pooling proxies (PgBouncer) or serverless-friendly databases

5. **Bundle Size**
   - 50 MB limit for serverless function code
   - Your NestJS backend is well under this limit

### Impact on Your Application

**What Works Well:**
- ✅ API routes for CRUD operations (transactions, holdings, portfolios)
- ✅ Authentication (JWT, Google OAuth2)
- ✅ CSV import/export
- ✅ Yahoo Finance API calls (external data fetching)

**What Needs Changes:**
- ⚠️ **DemoAccountCleanupScheduler**: Replace with Vercel Cron Jobs
- ⚠️ **Database Connection Pooling**: Use Prisma with connection pooling or serverless-compatible DB
- ⚠️ **Long-running operations**: Ensure CSV imports complete under timeout

**Overall Assessment:** ✅ **Vercel is a good fit for your application**. The constraints are manageable with proper configuration.

---

## Architecture Design: Monorepo with Next.js API Routes

### Current Structure
```
stocktracker/
├── frontend/               # Next.js 15 app
│   ├── src/
│   │   ├── app/
│   │   ├── components/
│   │   └── services/
│   ├── public/
│   ├── package.json
│   └── next.config.ts
└── backend-nodejs/         # Separate NestJS app
    ├── src/
    │   ├── auth/
    │   ├── user/
    │   └── database/
    ├── prisma/
    └── package.json
```

### Target Structure (Monorepo - Root Folder)
```
stocktracker/  (root)
├── src/
│   ├── app/
│   │   ├── api/                    # Backend API routes (NEW)
│   │   │   ├── auth/
│   │   │   │   ├── login/route.ts
│   │   │   │   ├── register/route.ts
│   │   │   │   └── oauth2/callback/google/route.ts
│   │   │   ├── portfolios/route.ts
│   │   │   ├── transactions/route.ts
│   │   │   └── ...
│   │   ├── dashboard/              # Frontend pages
│   │   ├── login/
│   │   ├── register/
│   │   ├── page.tsx
│   │   └── layout.tsx
│   ├── components/                 # UI components (from frontend)
│   │   ├── auth/
│   │   ├── dashboard/
│   │   └── common/
│   ├── services/api/               # Frontend API client (from frontend)
│   │   ├── axiosInstance.ts
│   │   ├── portfolioApi.ts
│   │   └── transactionApi.ts
│   ├── lib/                        # Backend logic (NEW - from backend-nodejs)
│   │   ├── auth/                   # Migrated from backend-nodejs/src/auth
│   │   │   ├── auth.service.ts
│   │   │   ├── jwt.service.ts
│   │   │   ├── dto/
│   │   │   └── strategies/
│   │   ├── database/               # Migrated from backend-nodejs/src/database
│   │   │   └── prisma.ts
│   │   ├── user/                   # Migrated from backend-nodejs/src/user
│   │   │   └── user.service.ts
│   │   └── common/                 # Migrated from backend-nodejs/src/common
│   │       ├── utils/
│   │       └── exceptions/
│   ├── hooks/                      # React hooks (from frontend)
│   ├── types/                      # TypeScript types (from frontend)
│   ├── utils/                      # Frontend utils (from frontend)
│   └── middleware.ts               # Next.js auth middleware (from frontend)
├── public/                         # Static assets (from frontend)
├── prisma/
│   └── schema.prisma               # Moved from backend-nodejs/prisma
├── package.json                    # Combined dependencies
├── next.config.ts                  # Next.js config (from frontend)
├── tsconfig.json                   # TypeScript config (merged)
├── tailwind.config.ts              # Tailwind config (from frontend)
├── vercel.json                     # Vercel deployment config (NEW)
└── .env.local                      # Environment variables
```

### Why This Structure?

1. **Single Deployment**: One `vercel.json`, one domain, one deployment
2. **No CORS Issues**: Frontend and backend share the same origin
3. **Code Sharing**: Types, utilities, validators shared between frontend/backend
4. **Next.js Optimization**: Automatic API route optimization, edge functions support

---

## Database Migration Strategy

### Chosen: **Supabase** (PostgreSQL)

**Why Supabase?**
- **PostgreSQL**: More powerful than MySQL (better JSON support, advanced indexing, CTEs)
- **Serverless-native**: Connection pooling via pgBouncer (no connection limit issues)
- **Free tier**: 500 MB database, 2 GB bandwidth, unlimited API requests
- **Built-in features**:
  - Authentication (could replace custom JWT in future)
  - Real-time subscriptions (for live portfolio updates)
  - Storage (for future CSV file uploads)
  - Auto-generated REST API (optional, won't use initially)
- **Developer-friendly**: Excellent dashboard, logging, SQL editor
- **Automatic backups**: Daily backups on free tier

**Migration Complexity**: Medium (MySQL → PostgreSQL requires schema adjustments)

### Schema Migration (MySQL → PostgreSQL)

#### Key Differences to Address

1. **Data Types**
   - `BIT(1)` → `BOOLEAN`
   - `DateTime @db.DateTime(6)` → `TIMESTAMP(3)` or `TIMESTAMPTZ`
   - `DECIMAL` → Same (PostgreSQL supports DECIMAL)
   - `BIGINT @default(autoincrement())` → `BIGSERIAL` or `BIGINT @default(autoincrement())`

2. **Enum Naming**
   - PostgreSQL requires schema-qualified enum names
   - MySQL: `transactions_type` → PostgreSQL: `"transactions_type"` (quoted)

3. **Foreign Key Constraints**
   - Syntax compatible, but PostgreSQL is stricter about cascade rules

#### Updated Prisma Schema for PostgreSQL

```prisma
generator client {
  provider      = "prisma-client-js"
  binaryTargets = ["native", "rhel-openssl-1.0.x"]  // For Vercel
}

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
  directUrl = env("DIRECT_URL")  // Supabase connection pooler
}

model holdings {
  id           BigInt    @id @default(autoincrement())
  created_at   DateTime  @default(now()) @db.Timestamp(6)
  updated_at   DateTime? @updatedAt @db.Timestamp(6)
  average_cost Decimal   @db.Decimal(10, 2)
  company_name String    @db.VarChar(100)
  shares       Decimal   @db.Decimal(12, 4)
  symbol       String    @db.VarChar(10)
  user_id      BigInt
  users        users     @relation(fields: [user_id], references: [id], onDelete: Cascade, onUpdate: NoAction)

  @@unique([user_id, symbol])
  @@index([user_id])
}

model transactions {
  id               BigInt            @id @default(autoincrement())
  created_at       DateTime          @default(now()) @db.Timestamp(6)
  updated_at       DateTime?         @updatedAt @db.Timestamp(6)
  company_name     String            @db.VarChar(100)
  notes            String?           @db.VarChar(500)
  price_per_share  Decimal           @db.Decimal(10, 2)
  shares           Decimal           @db.Decimal(12, 4)
  symbol           String            @db.VarChar(10)
  total_amount     Decimal           @db.Decimal(14, 2)
  transaction_date DateTime          @db.Date
  type             transactions_type
  user_id          BigInt
  broker_fee       Decimal?          @db.Decimal(10, 2)
  users            users             @relation(fields: [user_id], references: [id], onDelete: Cascade, onUpdate: NoAction)

  @@index([user_id, transaction_date])
  @@index([user_id, symbol])
}

model users {
  id                BigInt              @id @default(autoincrement())
  created_at        DateTime            @default(now()) @db.Timestamp(6)
  updated_at        DateTime?           @updatedAt @db.Timestamp(6)
  email             String              @unique @db.VarChar(255)
  enabled           Boolean             @default(true)  // Changed from BIT(1)
  name              String              @db.VarChar(255)
  password          String?             @db.VarChar(255)
  role              users_role          @default(USER)
  auth_provider     users_auth_provider @default(LOCAL)
  is_demo_account   Boolean             @default(false)  // Changed from BIT(1)
  oauth_provider_id String?             @db.VarChar(255)
  profile_image_url String?             @db.VarChar(500)
  holdings          holdings[]
  transactions      transactions[]

  @@index([oauth_provider_id, auth_provider])
}

enum users_role {
  USER
  ADMIN
}

enum users_auth_provider {
  LOCAL
  GOOGLE
}

enum transactions_type {
  BUY
  SELL
}
```

### Migration Steps

#### 1. Export Data from MySQL

**Export to SQL:**
```bash
mysqldump -u stocktracker -p \
  --no-create-info \
  --complete-insert \
  --skip-add-locks \
  --skip-comments \
  stocktracker > data-export.sql
```

**Or Export to CSV (recommended for PostgreSQL):**
```bash
mysql -u stocktracker -p stocktracker -e "SELECT * FROM users" > users.csv
mysql -u stocktracker -p stocktracker -e "SELECT * FROM holdings" > holdings.csv
mysql -u stocktracker -p stocktracker -e "SELECT * FROM transactions" > transactions.csv
```

#### 2. Create Supabase Project

1. Sign up at **supabase.com**
2. Create new project: `stocktracker-prod`
3. Choose region: **closest to your users**
4. Set strong database password
5. Wait for project initialization (~2 minutes)

#### 3. Get Connection Strings

Supabase provides two connection strings:

**Pooler (for Vercel serverless):**
```
DATABASE_URL="postgresql://postgres.xxxxx:password@aws-0-us-east-1.pooler.supabase.com:6543/postgres"
```

**Direct (for migrations):**
```
DIRECT_URL="postgresql://postgres.xxxxx:password@aws-0-us-east-1.compute.amazonaws.com:5432/postgres"
```

#### 4. Update Prisma Schema

- Change `provider = "mysql"` → `provider = "postgresql"`
- Add `directUrl = env("DIRECT_URL")`
- Update data types (BIT → BOOLEAN)
- Add `@default(now())` and `@updatedAt` where appropriate

#### 5. Push Schema to Supabase

```bash
cd stocktracker
export DATABASE_URL="postgresql://..."
export DIRECT_URL="postgresql://..."
npx prisma db push
```

This creates all tables, indexes, and enums in PostgreSQL.

#### 6. Import Data

**Option A: Using Supabase SQL Editor**

1. Open Supabase Dashboard → SQL Editor
2. Convert MySQL data to PostgreSQL INSERT statements
3. Run INSERT queries (handle BIT → BOOLEAN conversion)

**Option B: Using Prisma Seed Script**

Create `prisma/seed.ts`:
```typescript
import { PrismaClient } from '@prisma/client';
import * as fs from 'fs';
import * as csv from 'csv-parser';

const prisma = new PrismaClient();

async function main() {
  // Read CSV files and insert data
  const users = await readCSV('users.csv');
  for (const user of users) {
    await prisma.users.create({
      data: {
        id: BigInt(user.id),
        email: user.email,
        name: user.name,
        password: user.password,
        enabled: user.enabled === '1',  // Convert BIT to Boolean
        role: user.role,
        auth_provider: user.auth_provider,
        is_demo_account: user.is_demo_account === '1',
        // ... other fields
      },
    });
  }
  // Repeat for holdings and transactions
}

main();
```

Run: `npx tsx prisma/seed.ts`

**Option C: Manual CSV Import (Simplest)**

1. Use Supabase Table Editor
2. Select table → Import Data → Upload CSV
3. Map columns correctly
4. Handle Boolean conversions manually (0/1 → false/true)

#### 7. Verify Data Migration

```sql
-- In Supabase SQL Editor
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM holdings;
SELECT COUNT(*) FROM transactions;

-- Check data integrity
SELECT u.id, u.email, COUNT(h.id) as holdings_count
FROM users u
LEFT JOIN holdings h ON u.id = h.user_id
GROUP BY u.id, u.email;
```

#### 8. Test Locally

```bash
cd stocktracker
export DATABASE_URL="postgresql://...pooler.supabase.com:6543/postgres"
npm run dev
```

Test API routes to ensure database connection works.

---

## Implementation Plan

### Phase 0: Folder Restructuring

**Goal**: Move frontend to root folder and prepare for monorepo structure.

#### Steps

1. **Create new root structure**
   ```bash
   # Backup current structure first
   cd /mnt/d/projects/stocktracker
   git checkout -b refactor-monorepo

   # Move frontend contents to root
   mv frontend/src .
   mv frontend/public .
   mv frontend/package.json package.json
   mv frontend/next.config.ts .
   mv frontend/tsconfig.json .
   mv frontend/tailwind.config.ts .
   mv frontend/postcss.config.mjs .
   mv frontend/.eslintrc.json .
   mv frontend/.gitignore .gitignore-frontend
   cat .gitignore-frontend >> .gitignore

   # Move Prisma schema
   mv backend-nodejs/prisma .

   # Create new directories
   mkdir -p src/lib
   ```

2. **Update import paths**
   - All imports that referenced `@/` still work (Next.js tsconfig.json uses `"@/*": ["./src/*"]`)
   - Verify no broken imports

3. **Update package.json**
   - Merge dependencies from `backend-nodejs/package.json`
   - Update scripts if needed

4. **Test**
   ```bash
   npm install
   npm run dev
   ```

---

### Phase 1: Backend Refactoring

**Goal**: Convert NestJS modules into standalone service modules compatible with Next.js API routes.

#### Critical Files to Create/Modify

**1. Create Service Layer** (`src/lib/`)

Move and adapt these backend-nodejs modules:

- `src/lib/database/prisma.ts` - Singleton Prisma client for serverless
- `src/lib/auth/auth.service.ts` - Authentication logic (login, register, JWT)
- `src/lib/auth/jwt.service.ts` - Token generation/validation
- `src/lib/user/user.service.ts` - User CRUD operations
- `src/lib/common/utils/password-validator.ts` - Password validation
- `src/lib/common/exceptions/` - Custom error classes

**2. Create API Route Handlers** (`src/app/api/`)

New Next.js API routes (using `route.ts` convention):

- `src/app/api/auth/login/route.ts` - POST /api/auth/login
- `src/app/api/auth/register/route.ts` - POST /api/auth/register
- `src/app/api/auth/logout/route.ts` - POST /api/auth/logout
- `src/app/api/auth/oauth2/callback/google/route.ts` - GET /api/auth/oauth2/callback/google
- `src/app/api/portfolios/route.ts` - GET /api/portfolios (protected)
- `src/app/api/transactions/route.ts` - GET/POST /api/transactions (protected)

**3. Middleware Adaptation**

- Convert NestJS guards to Next.js middleware functions
- JWT verification in API routes using `headers().get('authorization')`
- Example:
  ```typescript
  // src/lib/middleware/auth.ts
  export async function requireAuth(request: Request) {
    const token = request.headers.get('authorization')?.split(' ')[1];
    if (!token) throw new UnauthorizedException();
    return await jwtService.verify(token);
  }
  ```

**4. Update Prisma Schema for PostgreSQL**

- Change `provider = "mysql"` → `provider = "postgresql"`
- Add `directUrl = env("DIRECT_URL")` for Supabase
- Change `BIT(1)` → `Boolean`
- Add `binaryTargets = ["native", "rhel-openssl-1.0.x"]` for Vercel

**5. Environment Variables**

Create `.env.local` at project root:
```env
# Supabase Database
DATABASE_URL=postgresql://postgres.xxxxx:password@aws-0-us-east-1.pooler.supabase.com:6543/postgres
DIRECT_URL=postgresql://postgres.xxxxx:password@aws-0-us-east-1.compute.amazonaws.com:5432/postgres

# JWT Configuration
JWT_SECRET=your-base64-secret
JWT_EXPIRATION=86400000

# Google OAuth2
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxxxxxxx
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth2/redirect

# External APIs
YAHOO_FINANCE_CHART_URL=https://query1.finance.yahoo.com/v8/finance/chart

# Frontend
NEXT_PUBLIC_API_URL=/api

# Cron Security
CRON_SECRET=generate-random-secure-string
```

---

### Phase 2: Database Setup (Supabase PostgreSQL)

1. **Create Supabase account** and project at supabase.com
2. **Get connection strings** (pooler and direct URLs)
3. **Update Prisma schema** for PostgreSQL (see schema above)
4. **Push schema to Supabase**: `npx prisma db push`
5. **Export data from MySQL**: Use mysqldump or CSV export
6. **Import data to Supabase**: Use SQL Editor, CSV import, or Prisma seed script
7. **Verify migration**: Check data counts and integrity
8. **Test connection** locally with new DATABASE_URL

---

### Phase 3: Scheduler Migration (Cron Jobs)

Replace `DemoAccountCleanupScheduler` with Vercel Cron Jobs.

**1. Create Cron API Route**
```typescript
// src/app/api/cron/cleanup-demo-accounts/route.ts
export async function GET(request: Request) {
  // Verify cron secret (security)
  const authHeader = request.headers.get('authorization');
  if (authHeader !== `Bearer ${process.env.CRON_SECRET}`) {
    return new Response('Unauthorized', { status: 401 });
  }

  // Run cleanup logic
  const service = new DemoAccountService();
  await service.cleanupExpiredAccounts();

  return Response.json({ success: true });
}
```

**2. Configure vercel.json**
```json
{
  "crons": [{
    "path": "/api/cron/cleanup-demo-accounts",
    "schedule": "0 2 * * *"  // 2 AM daily
  }]
}
```

---

### Phase 4: Frontend API Client Updates

**Update** `src/services/api/axiosInstance.ts`:

```typescript
const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || '/api',  // Relative URLs
  withCredentials: true,
});
```

**Remove** proxy rewrites from `next.config.ts`:
- Delete `/api/:path*` rewrite (API routes are now internal)
- Keep OAuth2 rewrite only if needed for Google redirect

---

### Phase 5: Vercel Deployment Configuration

**1. Create `vercel.json`**
```json
{
  "buildCommand": "prisma generate && next build",
  "installCommand": "npm install",
  "framework": "nextjs",
  "regions": ["iad1"],
  "env": {
    "DATABASE_URL": "@database-url",
    "JWT_SECRET": "@jwt-secret",
    "GOOGLE_CLIENT_ID": "@google-client-id",
    "GOOGLE_CLIENT_SECRET": "@google-client-secret"
  },
  "crons": [{
    "path": "/api/cron/cleanup-demo-accounts",
    "schedule": "0 2 * * *"
  }]
}
```

**2. Add to `package.json`**
```json
{
  "scripts": {
    "build": "prisma generate && next build",
    "postinstall": "prisma generate"
  }
}
```

**3. Vercel Environment Variables**

Add these in Vercel Dashboard → Settings → Environment Variables:
- `DATABASE_URL` (PlanetScale connection string)
- `JWT_SECRET`
- `JWT_EXPIRATION`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `OAUTH2_REDIRECT_URI` (https://yourdomain.vercel.app/oauth2/redirect)
- `YAHOO_FINANCE_CHART_URL`
- `CRON_SECRET` (generate random string for cron security)

**4. Deploy**
```bash
vercel --prod
```

---

## File Migration Checklist

### Phase 0: Folder Restructuring

**Move frontend to root:**

- [ ] Move `frontend/src/` → `src/`
- [ ] Move `frontend/public/` → `public/`
- [ ] Move `frontend/package.json` → `package.json`
- [ ] Move `frontend/next.config.ts` → `next.config.ts`
- [ ] Move `frontend/tsconfig.json` → `tsconfig.json`
- [ ] Move `frontend/tailwind.config.ts` → `tailwind.config.ts`
- [ ] Move `frontend/postcss.config.mjs` → `postcss.config.mjs`
- [ ] Move `frontend/.eslintrc.json` → `.eslintrc.json`
- [ ] Merge `frontend/.gitignore` → `.gitignore`
- [ ] Move `backend-nodejs/prisma/` → `prisma/`

### Phase 1: Backend Logic Migration

**From `backend-nodejs/src/`** → **To `src/lib/`**

- [ ] `database/prisma.service.ts` → `src/lib/database/prisma.ts` (add singleton pattern)
- [ ] `auth/auth.service.ts` → `src/lib/auth/auth.service.ts`
- [ ] `auth/jwt.service.ts` → `src/lib/auth/jwt.service.ts`
- [ ] `auth/strategies/*` → `src/lib/auth/strategies/*` (adapt for Next.js)
- [ ] `auth/dto/*` → `src/lib/auth/dto/*`
- [ ] `user/user.service.ts` → `src/lib/user/user.service.ts`
- [ ] `common/utils/password-validator.ts` → `src/lib/common/utils/password-validator.ts`
- [ ] `common/exceptions/*` → `src/lib/common/exceptions/*`
- [ ] `common/decorators/current-user.decorator.ts` → `src/lib/middleware/auth.ts` (refactor)

### Phase 1: API Routes to Create

**In `src/app/api/`**

- [ ] `auth/login/route.ts` - Login handler
- [ ] `auth/register/route.ts` - Registration handler
- [ ] `auth/logout/route.ts` - Logout handler
- [ ] `auth/oauth2/callback/google/route.ts` - Google OAuth callback
- [ ] `portfolios/route.ts` - Portfolio data endpoint
- [ ] `transactions/route.ts` - Transactions CRUD endpoint
- [ ] `cron/cleanup-demo-accounts/route.ts` - Cron job endpoint

### Configuration Files

- [ ] Update `prisma/schema.prisma` for PostgreSQL
- [ ] Create `vercel.json` (deployment config)
- [ ] Create `.env.local` with all environment variables (Supabase URLs)
- [ ] Update `package.json` with backend dependencies
- [ ] Remove proxy rewrites from `next.config.ts`

---

## Dependencies to Add

**Merge into root `package.json`:**

```json
{
  "dependencies": {
    "@prisma/client": "^5.22.0",
    "bcrypt": "^6.0.0",
    "jsonwebtoken": "^9.0.2",
    "passport": "^0.7.0",
    "passport-google-oauth20": "^2.0.0",
    "passport-jwt": "^4.0.1",
    "class-validator": "^0.14.3",
    "class-transformer": "^0.5.1"
  },
  "devDependencies": {
    "prisma": "^5.22.0",
    "@types/bcrypt": "^5.0.2",
    "@types/jsonwebtoken": "^9.0.5",
    "@types/passport-google-oauth20": "^2.0.16",
    "@types/passport-jwt": "^4.0.1"
  }
}
```

---

## Key Architectural Changes

### 1. Prisma Client Singleton Pattern

NestJS manages Prisma as a service. In Next.js API routes, use a singleton to prevent connection exhaustion:

```typescript
// src/lib/database/prisma.ts
import { PrismaClient } from '@prisma/client';

const globalForPrisma = global as unknown as { prisma: PrismaClient };

export const prisma = globalForPrisma.prisma || new PrismaClient({
  log: ['error'],
  // Supabase connection pooling is handled via DATABASE_URL
});

if (process.env.NODE_ENV !== 'production') globalForPrisma.prisma = prisma;
```

**Note**: Supabase handles connection pooling automatically via the pooler URL. No need for manual connection management.

### 2. Authentication Middleware

Convert NestJS guards to middleware functions:

```typescript
// src/lib/middleware/auth.ts
import { jwtService } from '@/lib/auth/jwt.service';

export async function authenticateRequest(request: Request) {
  const authHeader = request.headers.get('authorization');
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    throw new Error('Unauthorized');
  }

  const token = authHeader.split(' ')[1];
  const payload = await jwtService.verify(token);
  return payload;  // Contains userId, email, etc.
}
```

### 3. API Route Example

```typescript
// src/app/api/portfolios/route.ts
import { authenticateRequest } from '@/lib/middleware/auth';
import { portfolioService } from '@/lib/portfolio/portfolio.service';

export async function GET(request: Request) {
  try {
    const user = await authenticateRequest(request);
    const portfolios = await portfolioService.getPortfolios(user.userId);
    return Response.json({ success: true, data: portfolios });
  } catch (error) {
    return Response.json({ success: false, error: error.message }, { status: 401 });
  }
}
```

---

## Testing Strategy

### Local Testing (Before Vercel)

1. **Update environment variables** in `frontend/.env.local`
2. **Run Prisma migrations**: `npx prisma db push`
3. **Start Next.js dev server**: `npm run dev`
4. **Test API routes**:
   - http://localhost:3000/api/auth/login
   - http://localhost:3000/api/portfolios
5. **Verify frontend** still works with new API structure

### Vercel Preview Testing

1. **Deploy to Vercel preview**: `vercel`
2. **Test OAuth2 flow** with updated redirect URI
3. **Verify database connection** from Vercel functions
4. **Check function logs** in Vercel dashboard

### Production Deployment

1. **Backup current data** from MySQL
2. **Deploy to production**: `vercel --prod`
3. **Update DNS** if using custom domain
4. **Monitor**: Check Vercel logs for errors

---

## Rollback Plan

If deployment fails:

1. **Keep backend-nodejs running** on current server
2. **Revert frontend** to use proxy rewrites to old backend
3. **Investigate errors** in Vercel function logs
4. **Fix issues** in preview deployment before re-attempting production

---

## Timeline Considerations

**Complexity**: Medium-High
**Estimated Effort**:
- Folder restructuring: Minor (moving files, updating paths)
- Backend refactoring: Major (restructuring NestJS to Next.js API routes)
- Database migration: Medium (MySQL → PostgreSQL schema changes + data import)
- Testing: Medium (API routes, OAuth, database connections)

**Recommended Approach**: Incremental migration
1. Restructure folders first (Phase 0)
2. Set up Supabase database and migrate schema (Phase 1)
3. Migrate one API route at a time, starting with auth (Phase 2)
4. Test each route thoroughly before moving on
5. Deploy to Vercel preview frequently

---

## Potential Issues & Solutions

### Issue 1: Database Connection Pooling
**Problem**: Too many database connections from serverless functions
**Solution**: Supabase provides connection pooling via pgBouncer automatically through the pooler URL

### Issue 2: OAuth2 Redirect URI
**Problem**: Google OAuth requires exact redirect URI
**Solution**: Update Google Cloud Console with Vercel production URL

### Issue 3: Cold Start Latency
**Problem**: First API call after inactivity is slow
**Solution**:
- Use Vercel Pro (faster cold starts)
- Implement keep-alive pinging (optional)
- Set user expectations

### Issue 4: Environment Variables Not Loading
**Problem**: Build fails, missing env vars
**Solution**: Ensure all vars are set in Vercel dashboard, not just .env.local

### Issue 5: Supabase Database Pausing (Free Tier)
**Problem**: Database pauses after 1 week of inactivity, first request after pause is slow
**Solution**:
- Upgrade to Pro ($25/month) to remove pausing
- Or accept the pause behavior for personal projects
- Database wakes up automatically on first request

### Issue 6: MySQL to PostgreSQL Data Type Conversion
**Problem**: Boolean values stored as BIT(1) in MySQL need conversion to PostgreSQL BOOLEAN
**Solution**:
- During data import, convert `0` → `false`, `1` → `true`
- Use Prisma seed script or manual CSV conversion
- Test data integrity after import

---

## Cost Estimate

**Vercel**:
- Hobby Plan: $0 (100 GB bandwidth, 100 GB-hrs serverless execution)
- Pro Plan: $20/month (if you need faster cold starts, more bandwidth)

**Supabase**:
- Free Plan: $0 (500 MB database, 2 GB bandwidth, 50k monthly active users)
- Pro Plan: $25/month (8 GB database, 250 GB bandwidth, no pausing)

**Total**: $0-20/month for Vercel + $0-25/month for database = **$0-45/month**

**Free Tier Limits**:
- Supabase: 500 MB database (your current data is likely < 100 MB)
- Database pauses after 1 week of inactivity (Pro removes this)
- Should be sufficient for personal use and moderate traffic

Compare to current homelab: Electricity + hardware maintenance savings likely offset this. Plus better uptime and global CDN.

---

## Verification Steps (Post-Deployment)

### 1. Authentication
- [ ] Register new user via `/api/auth/register`
- [ ] Login with email/password via `/api/auth/login`
- [ ] Login with Google OAuth via `/oauth2/authorize/google`
- [ ] Verify JWT token in cookies
- [ ] Access protected route `/api/portfolios` with token

### 2. Portfolio Operations
- [ ] Create new portfolio
- [ ] Add transaction (BUY)
- [ ] View holdings
- [ ] CSV import transactions
- [ ] Export portfolio data

### 3. Performance
- [ ] Test cold start latency (first API call)
- [ ] Test warm instance latency (subsequent calls)
- [ ] Check Vercel function execution time (should be < 2s)

### 4. Database
- [ ] Verify data persisted in PlanetScale
- [ ] Test transaction rollback
- [ ] Check connection count in PlanetScale dashboard

### 5. Cron Jobs
- [ ] Manually trigger `/api/cron/cleanup-demo-accounts` (with CRON_SECRET)
- [ ] Wait for scheduled run (check Vercel Cron logs)
- [ ] Verify demo accounts cleaned up

---

## Next Steps After Plan Approval

1. **Phase 0: Folder Restructuring**
   - Create new branch: `refactor-monorepo`
   - Move frontend files to root
   - Test that Next.js still works

2. **Phase 1: Database Setup**
   - Create Supabase account and project
   - Export data from current MySQL
   - Update Prisma schema for PostgreSQL
   - Push schema to Supabase
   - Import data

3. **Phase 2: Backend Refactoring**
   - Create `src/lib/` structure
   - Migrate auth module first (highest priority)
   - Create Next.js API routes
   - Test locally with new API routes

4. **Phase 3: Deployment**
   - Create `vercel.json`
   - Set environment variables in Vercel
   - Deploy to Vercel preview
   - Test thoroughly
   - Production deployment

5. **Phase 4: Cleanup**
   - Archive `backend-nodejs/` folder
   - Update README with new deployment instructions
   - Document new architecture

---

## Summary

This plan transforms your Stock Tracker application from a split frontend/backend architecture into a **unified Vercel-ready monorepo** with the following key changes:

### Key Decisions

✅ **Monorepo Structure**: Move frontend to root folder, merge with backend logic in `src/lib/`
✅ **Database**: Migrate from MySQL to Supabase PostgreSQL
✅ **Deployment**: Single Vercel project with Next.js API routes
✅ **Cost**: $0 with free tiers (Vercel Hobby + Supabase Free)

### Critical Files

**To Create/Modify:**
- `prisma/schema.prisma` - Update for PostgreSQL
- `src/lib/database/prisma.ts` - Singleton Prisma client
- `src/lib/auth/*.ts` - Authentication services
- `src/app/api/*/route.ts` - Next.js API routes
- `vercel.json` - Deployment configuration
- `.env.local` - Environment variables (Supabase URLs)

### Success Criteria

- [ ] Frontend moved to root folder, Next.js still works
- [ ] Data migrated from MySQL to Supabase PostgreSQL
- [ ] All API routes functional (auth, portfolios, transactions)
- [ ] Google OAuth working with new redirect URI
- [ ] Cron job for demo account cleanup configured
- [ ] Deployed to Vercel and accessible
- [ ] Tests passing, no regressions

### Risk Mitigation

- Incremental migration (one module at a time)
- Test locally before Vercel deployment
- Use Vercel preview deployments before production
- Keep current homelab setup as rollback option

