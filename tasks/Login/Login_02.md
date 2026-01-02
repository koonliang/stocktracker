# Login_02: Demo Account Auto-Login

## Goal
Wire "Watch Demo" button to create a fresh demo account with seeded portfolio data. Each demo login creates a new user, cleaned up nightly after 24 hours.

---

## Implementation Steps

### Step 1: Add `isDemoAccount` flag to User entity
**File:** `backend/src/main/java/com/stocktracker/entity/User.java`

Add after `enabled` field:
```java
@Column(name = "is_demo_account", nullable = false)
@Builder.Default
private boolean isDemoAccount = false;
```

---

### Step 2: Add repository methods for cleanup
**File:** `backend/src/main/java/com/stocktracker/repository/UserRepository.java`
```java
List<User> findByIsDemoAccountTrueAndCreatedAtBefore(LocalDateTime cutoffTime);
```

**File:** `backend/src/main/java/com/stocktracker/repository/TransactionRepository.java`
```java
void deleteByUserId(Long userId);
```

**File:** `backend/src/main/java/com/stocktracker/repository/HoldingRepository.java`
```java
void deleteByUserId(Long userId);
```

---

### Step 3: Create DemoAccountService
**New file:** `backend/src/main/java/com/stocktracker/service/DemoAccountService.java`

Responsibilities:
- Generate unique demo email: `demo-{uuid}@stocktracker.demo`
- Create user with `isDemoAccount = true`
- Seed 6 stocks (AAPL, MSFT, GOOGL, TSLA, NVDA, AMZN) with BUY/SELL transactions
- Trigger holding recalculation via `HoldingRecalculationService`

---

### Step 4: Add demoLogin to AuthService
**File:** `backend/src/main/java/com/stocktracker/service/AuthService.java`

- Inject `DemoAccountService`
- Add `demoLogin()` method that:
  1. Calls `demoAccountService.createDemoAccount()`
  2. Generates JWT for the new user
  3. Returns `AuthResponse`

---

### Step 5: Add demo-login endpoint
**File:** `backend/src/main/java/com/stocktracker/controller/AuthController.java`

```java
@PostMapping("/demo-login")
public ResponseEntity<ApiResponse<AuthResponse>> demoLogin() {
    AuthResponse response = authService.demoLogin();
    return ResponseEntity.ok(ApiResponse.success("Demo account created", response));
}
```

---

### Step 6: Create nightly cleanup scheduler
**File:** `backend/src/main/java/com/stocktracker/StocktrackerApplication.java`
- Add `@EnableScheduling` annotation

**New file:** `backend/src/main/java/com/stocktracker/scheduler/DemoAccountCleanupScheduler.java`
- Run at 2 AM daily: `@Scheduled(cron = "0 0 2 * * *")`
- Find demo accounts older than 24 hours
- Delete in order: Holdings -> Transactions -> Users (FK constraints)

---

### Step 7: Frontend - Add demoLogin to authService
**File:** `frontend/src/services/authService.ts`

Add method:
```typescript
async demoLogin(): Promise<AuthResponse> {
  const response = await api.post<ApiResponse<AuthResponse>>('/auth/demo-login')
  // Store token and user in localStorage (same as login)
  return authData
}
```

---

### Step 8: Frontend - Wire Watch Demo button
**File:** `frontend/src/pages/Home/Home.tsx`

- Add `useState` for loading state
- Add `handleDemoLogin` async function
- Replace button with loading spinner while creating demo

---

## Files Summary

| Action | File |
|--------|------|
| Modify | `backend/.../entity/User.java` |
| Modify | `backend/.../repository/UserRepository.java` |
| Modify | `backend/.../repository/TransactionRepository.java` |
| Modify | `backend/.../repository/HoldingRepository.java` |
| Create | `backend/.../service/DemoAccountService.java` |
| Modify | `backend/.../service/AuthService.java` |
| Modify | `backend/.../controller/AuthController.java` |
| Modify | `backend/.../StocktrackerApplication.java` |
| Create | `backend/.../scheduler/DemoAccountCleanupScheduler.java` |
| Modify | `frontend/src/services/authService.ts` |
| Modify | `frontend/src/pages/Home/Home.tsx` |

---

## Demo Stock Seed Data

| Symbol | Company | Buy Shares | Buy Price | Sell Shares | Sell Price |
|--------|---------|------------|-----------|-------------|------------|
| AAPL | Apple Inc. | 60 | $142.50 | 10 | $150.00 |
| MSFT | Microsoft Corporation | 30 | $285.00 | 5 | $320.00 |
| GOOGL | Alphabet Inc. | 10 | $125.30 | 0 | - |
| TSLA | Tesla, Inc. | 20 | $248.00 | 5 | $265.00 |
| NVDA | NVIDIA Corporation | 20 | $450.00 | 0 | - |
| AMZN | Amazon.com, Inc. | 40 | $135.00 | 10 | $145.00 |

---

## Future Considerations
- Rate limiting on demo-login endpoint to prevent abuse
- UI banner indicating "Demo Mode - Data resets after 24h"
