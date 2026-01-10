# Implementation Plan: Backend Migration from Java/Spring Boot to Node.js/NestJS

## Overview
Migrate StockTracker backend from Java/Spring Boot to Node.js/NestJS while maintaining 100% API compatibility with the existing Next.js frontend. All calculations, validations, and business logic must match exactly.

## Implementation Order

### Phase 1: Foundation & Database (Modules 1-4)
**Goal**: Setup project structure, database, and common utilities

1. **Initialize NestJS Project**
   - Create `backend-nodejs` directory
   - Install dependencies: `@nestjs/config`, `@nestjs/passport`, `@nestjs/jwt`, `@nestjs/cache-manager`, `@nestjs/schedule`, `@nestjs/swagger`, `passport-jwt`, `passport-local`, `passport-google-oauth20`, `@prisma/client`, `prisma`, `class-validator`, `class-transformer`, `bcrypt`, `axios`, `cache-manager`, `decimal.js`
   - Configure TypeScript strict mode

2. **Setup Prisma**
   - Create schema matching MySQL database (User, Transaction, Holding with exact field types)
   - Configure Prisma Client with decimal precision
   - Run migration: `npx prisma migrate dev --name init`

3. **Create Common Module**
   - Exception filter matching Java's GlobalExceptionHandler
   - Custom exceptions: BadRequestException, ResourceNotFoundException
   - API response DTO wrapper
   - @CurrentUser() decorator for JWT user extraction
   - Transform interceptor for consistent responses

4. **Create User Module**
   - UserService with Prisma CRUD operations
   - No controllers yet - just service layer

**Verification**: Server starts, database connects, health endpoint works

---

### Phase 2: Authentication & Security (Modules 5-6)
**Goal**: Implement JWT and OAuth2 authentication matching Java backend

5. **JWT Authentication**
   - Configure JwtModule: HS256 algorithm, 24h expiration, BASE64-encoded secret
   - Create JwtStrategy matching Java's JwtTokenProvider
   - Create JwtAuthGuard
   - **Critical**: Token generation must be compatible with Java backend

6. **Auth Module**
   - AuthService: login, register, demoLogin methods
   - AuthController: POST /api/auth/login, /register, /logout, /demo-login
   - GoogleStrategy for OAuth2: GET /oauth2/authorize/google, /api/auth/oauth2/callback/google
   - Password validation: min 8 chars, max 72 chars, uppercase, lowercase, digit, special char
   - Use bcrypt with default rounds (10)

**Verification**: Login works, JWT tokens valid, OAuth2 flow completes, password validation matches

---

### Phase 3: External Integration (Module 7)
**Goal**: Implement Yahoo Finance API client

7. **Yahoo Finance Client**
   - YahooFinanceClient with axios
   - Methods: getQuotes(), getHistoricalData(), getHistoricalDataBatch()
   - Parallel requests with Promise.all()
   - Timeouts: connect=5s, read=10s
   - Parse v8 chart API: chart.result[0].indicators.quote[0] for OHLCV data
   - Handle null prices gracefully

**Verification**: Can fetch quotes for AAPL/MSFT/GOOGL, batch requests work in parallel

**Reference**: `/mnt/d/projects/stocktracker/backend/src/main/java/com/stocktracker/client/YahooFinanceClient.java`

---

### Phase 4: Core Business Logic (Modules 8-10)
**Goal**: Implement transactions, holdings, and CSV import

8. **Transaction Module**
   - TransactionService with CRUD operations
   - Sell validation: check buy exists, date not before first buy, enough shares
   - Ticker validation via Yahoo Finance
   - CSV export with proper escaping
   - **Critical**: Use decimal.js for all arithmetic: `totalAmount = shares.mul(pricePerShare).add(brokerFee || 0)`
   - Cache eviction on mutations

**Endpoints**: GET/POST /api/transactions, PUT/DELETE /api/transactions/:id, GET /api/transactions/validate-ticker, GET /api/transactions/export

9. **Holding Recalculation Service**
   - Implement weighted average cost calculation matching Java
   - **Critical algorithm**:
     ```typescript
     // BUY: totalCost += shares * price, totalShares += shares
     // SELL: avgCost = totalCost / totalShares (4 decimals)
     //       costReduction = soldShares * avgCost
     //       totalCost -= costReduction, totalShares -= soldShares
     // Final: averageCost = totalCost / totalShares (2 decimals, HALF_UP)
     ```
   - Delete holding if totalShares <= 0

10. **CSV Import Service**
    - Fuzzy field matching using Levenshtein distance (3-tier: exact→contains→edit distance)
    - Field aliases: 40+ variations for type, symbol, date, shares, price, fee, notes
    - Exchange suffix mapping: LSE→.L, SEHK→.HK, TSE→.TO, ASX→.AX, etc. (16 total)
    - Date parsing: 8 formats (M/d/yyyy, MM/dd/yyyy, yyyy-MM-dd, dd-MMM-yyyy, etc.)
    - IBKR pattern: negative shares → infer SELL, convert to positive
    - Type mappings: buy/b/purchase→BUY, sell/s/sale→SELL

**Endpoints**: POST /api/transactions/import/suggest-mapping, /import/preview, /import

**Verification**: CRUD works, sell validation prevents invalid transactions, CSV import handles IBKR format

**Reference**:
- `/mnt/d/projects/stocktracker/backend/src/main/java/com/stocktracker/service/TransactionService.java`
- `/mnt/d/projects/stocktracker/backend/src/main/java/com/stocktracker/service/HoldingRecalculationService.java`
- `/mnt/d/projects/stocktracker/backend/src/main/java/com/stocktracker/service/CsvImportService.java`

---

### Phase 5: Portfolio & Analytics (Module 11)
**Goal**: Implement complex portfolio calculations

11. **Portfolio Service**
    - **Current Value & Cost Basis**:
      ```typescript
      currentValue = lastPrice * shares (2 decimals, HALF_UP)
      costBasis = avgCost * shares (2 decimals, HALF_UP)
      returnPercent = (currentValue - costBasis) / costBasis * 100 (4 decimals, HALF_UP)
      weight = currentValue / totalPortfolioValue * 100 (4 decimals, HALF_UP)
      ```

    - **7-Day Return**:
      ```typescript
      change = currentPrice - price7DaysAgo
      changePercent = change / price7DaysAgo * 100 (4 decimals, HALF_UP)
      dollarReturn = change * shares
      // Historical data array: index 0 = oldest (7 days ago)
      ```

    - **CAGR (Annualized Yield)**:
      ```typescript
      years = daysBetween / 365.25
      if (years < 0.1) return totalReturnPercent // Less than 36 days
      totalReturnDecimal = totalReturnPercent / 100 (6 decimals)
      annualized = ((1 + totalReturnDecimal) ^ (1/years)) - 1
      annualizedPercent = annualized * 100 (2 decimals, HALF_UP)
      ```

    - **Sparkline Data**: Downsample to ~52 points: `step = max(1, pricesLength / 52)`

    - **Performance History**:
      - Calculate shares owned at each historical date (transaction-based)
      - For each date: sum all (shares × price at that date)

    - **Caching**: 2-minute TTL for portfolio, 10-minute for performance history

**Endpoints**: GET /api/portfolio, GET /api/portfolio/refresh, GET /api/portfolio/performance?range=

**Verification**: All calculations match Java exactly (compare with same test data), CAGR formula correct, performance uses transaction dates

**Reference**: `/mnt/d/projects/stocktracker/backend/src/main/java/com/stocktracker/service/PortfolioService.java`

---

### Phase 6: Demo Accounts & Scheduler (Module 12)
**Goal**: Implement demo accounts and cleanup job

12. **Demo Account Module**
    - Create demo account: email = "demo-{UUID}@stocktracker.demo"
    - Seed 12 transactions: AAPL (buy 60 @ 142.50, sell 10 @ 150), MSFT (buy 30 @ 285, sell 5 @ 320), GOOGL (buy 10 @ 125.30), TSLA (buy 20 @ 248, sell 5 @ 265), NVDA (buy 20 @ 450), AMZN (buy 40 @ 135, sell 10 @ 145)
    - Base date: 90 days ago, offset by +0, +5, +10, +15, +20, +25, +30, +35, +45, +55 days
    - Recalculate holdings after seeding

13. **Demo Cleanup Scheduler**
    - @Cron('0 0 * * * *') - Every hour at minute 0
    - Delete accounts with: isDemoAccount=true AND createdAt < (now - 24 hours)
    - Cascade delete transactions and holdings

**Verification**: Demo login creates account with seeded data, scheduler runs hourly, old accounts deleted

**Reference**:
- `/mnt/d/projects/stocktracker/backend/src/main/java/com/stocktracker/service/DemoAccountService.java`
- `/mnt/d/projects/stocktracker/backend/src/main/java/com/stocktracker/scheduler/DemoAccountCleanupScheduler.java`

---

## Critical Implementation Notes

### Decimal Precision (CRITICAL)
- **Use decimal.js for ALL financial calculations**
- Rounding mode: `Decimal.ROUND_HALF_UP` matches Java's `RoundingMode.HALF_UP`
- Precision requirements:
  - Shares: 12 digits, 4 decimals
  - Price: 10 digits, 2 decimals
  - Percentages: 4 decimals for calculations, 2 for display
  - Money: 2 decimals
- Example:
  ```typescript
  import Decimal from 'decimal.js';
  Decimal.set({ rounding: Decimal.ROUND_HALF_UP });

  const shares = new Decimal('100.5555');
  const price = new Decimal('150.75');
  const total = shares.mul(price).toDecimalPlaces(2); // 15160.16
  ```

### Date Handling
- Store dates in UTC, parse to start-of-day
- Use day.js or date-fns for consistency
- Transaction dates: YYYY-MM-DD format (LocalDate)
- Test all 8 CSV date formats
- Performance history excludes today

### Caching Strategy
- Portfolio cache key: `portfolio:{userId}` (2 min TTL)
- Performance cache key: `performanceHistory:{userId}:{range}` (10 min TTL)
- Evict both caches on any transaction mutation
- Use cache-manager with in-memory store (matches Java's Caffeine)

### API Response Format
All responses must match Java's structure exactly:
- AuthResponse: `{ token, type: "Bearer", userId, email, name }`
- PortfolioResponse: `{ holdings[], totalValue, totalCost, totalReturnPercent, totalReturnDollars, annualizedYield, investmentYears }`
- HoldingResponse: `{ symbol, companyName, shares, averageCost, lastPrice, currentValue, costBasis, returnPercent, returnDollars, weight, sevenDayReturnPercent, sevenDayReturnDollars, sparklineData[] }`
- TransactionResponse: `{ id, type, symbol, companyName, transactionDate, shares, pricePerShare, brokerFee, totalAmount, notes }`

---

## Risk Areas & Mitigation

### Risk 1: Calculation Precision
**Mitigation**: Use decimal.js for all financial math, create unit tests comparing Node.js vs Java outputs

### Risk 2: Date Handling
**Mitigation**: Test with DST transitions, different timezones, all CSV date formats

### Risk 3: Levenshtein Performance
**Mitigation**: Implement efficient algorithm with early exit, cache results, limit max string length

### Risk 4: OAuth2 Flow
**Mitigation**: Match exact redirect URIs, test edge cases (user denies, duplicate email)

### Risk 5: Yahoo Finance API
**Mitigation**: Add extensive error handling, cache aggressively, monitor for API changes

### Risk 6: Race Conditions
**Mitigation**: Use Prisma transactions for atomic operations, test concurrent imports

---

## Validation Steps

### Step 1: API Compatibility Test
Compare JSON responses between Java and Node.js backends:
```bash
# Test all endpoints with identical requests
# Verify field names, types, decimal precision match
```

### Step 2: Calculation Accuracy Test
Create identical portfolios in both backends:
```typescript
// Verify: totalValue, returnPercent, CAGR, 7-day return, weights all match
```

### Step 3: CSV Import Test
Export CSV from Java, import to Node.js:
```bash
# Verify: fuzzy matching, exchange suffixes, date parsing, IBKR pattern all match
```

### Step 4: Frontend Integration Test
Run frontend against Node.js backend:
```bash
# Update NEXT_PUBLIC_API_URL, verify: login, portfolio, transactions, CSV import all work
```

### Step 5: E2E Test Suite
```bash
npm run test:e2e
# 100% pass rate required
```

### Step 6: Load Test
```bash
# Portfolio endpoint: 100+ concurrent users
# CSV import: 1000 rows
# Yahoo Finance: 50+ symbols in parallel
```

---

## Critical Files Reference

| File | Purpose |
|------|---------|
| `backend/src/main/java/com/stocktracker/service/PortfolioService.java` | Portfolio calculations, CAGR, 7-day returns, sparklines, performance history |
| `backend/src/main/java/com/stocktracker/service/HoldingRecalculationService.java` | Weighted average cost calculation (FIFO method) |
| `backend/src/main/java/com/stocktracker/service/CsvImportService.java` | Fuzzy matching, field aliases, exchange mapping, date parsing |
| `backend/src/main/java/com/stocktracker/client/YahooFinanceClient.java` | Yahoo Finance API integration, OHLCV parsing |
| `backend/src/main/java/com/stocktracker/service/TransactionService.java` | CRUD, sell validation, CSV export |
| `backend/src/main/java/com/stocktracker/config/SecurityConfig.java` | JWT config, OAuth2 flow, endpoint protection |
| `backend/src/main/java/com/stocktracker/service/DemoAccountService.java` | Demo account creation with seeded transactions |

---

## Environment Variables

Create `backend-nodejs/.env.template`:
```
DATABASE_URL=mysql://user:password@localhost:3306/stocktracker
JWT_SECRET=your-secret-key-base64-encoded
JWT_EXPIRATION=86400000
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth2/redirect
YAHOO_FINANCE_CHART_URL=https://query1.finance.yahoo.com/v8/finance/chart
FRONTEND_URL=http://localhost:3000
PORT=8080
```

---

## Migration Execution Strategy

### Phase 1: Parallel Run
1. Deploy Node.js backend to staging
2. Run full test suite
3. Deploy to production (separate port)
4. Route 10% traffic to Node.js (feature flag or load balancer)

### Phase 2: Ramp Up
1. Increase to 25%, 50%, 75%
2. Monitor error rates and performance at each stage
3. Keep Java backend as fallback

### Phase 3: Full Cutover
1. Route 100% to Node.js
2. Keep Java running for 1 week
3. Decommission Java backend

### Rollback Plan
- Load balancer switches traffic in <1 minute
- Both backends use same MySQL database (no migration needed)
- Feature flag toggles backend in real-time

---

## Success Criteria

- [ ] All endpoints return identical JSON to Java backend
- [ ] All calculations match to correct decimal places
- [ ] Frontend works without modifications
- [ ] 100% E2E test pass rate
- [ ] Performance within 20% of Java backend
- [ ] Demo account scheduler runs reliably
- [ ] CSV import handles all test cases
- [ ] OAuth2 flow completes successfully
- [ ] No memory leaks after load testing
- [ ] Documentation complete

---

## Estimated Timeline

- Week 1: Foundation & Database (Phase 1)
- Week 2: Authentication (Phase 2)
- Week 3: Yahoo Finance & Transactions (Phase 3-4)
- Week 4: Holdings & CSV Import (Phase 4)
- Week 5: Portfolio & Analytics (Phase 5)
- Week 6: Demo Accounts & Testing (Phase 6)
- Week 7: Validation & Integration
- Week 8: Production Readiness

**Total**: 8 weeks (1 developer) or 4-5 weeks (2 developers in parallel)
