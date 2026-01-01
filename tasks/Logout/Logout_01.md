# Logout Feature Implementation - Enhanced Specification

## Overview
Implement a comprehensive logout feature with proper user experience, security handling, and error management. This includes logout UI in navigation, dedicated logout page, and improved session expiry handling.

---

## Current State Analysis

### Existing Authentication Infrastructure
- **Token Storage**: JWT tokens stored in `localStorage` under key `'authToken'`
- **User Data**: User info stored in `localStorage` under key `'user'`
- **Auth Service**: `authService.logout()` method exists but is not connected to UI
- **Interceptor**: Axios response interceptor handles 401 errors by redirecting to `/login`
- **Gap**: No handling for 403 (Forbidden) errors
- **Gap**: No logout UI/button in navigation
- **Gap**: No dedicated logout success page

### Current Routes
```
/ - Home
/login - Login page
/dashboard - Dashboard (no route guard)
/portfolios - Portfolios (no route guard)
/watchlist - Watchlist (no route guard)
```

---

## Phase 1: Backend Enhancements

### 1.1 Add Logout Endpoint (Optional but Recommended)
**File**: `backend/src/main/java/com/stocktracker/controller/AuthController.java`

**Implementation**:
```java
@PostMapping("/logout")
public ResponseEntity<ApiResponse<Void>> logout(
    @RequestHeader("Authorization") String authHeader
) {
    // Extract token from Bearer header
    // Optionally: Add token to blacklist/revocation list
    // For stateless JWT: client-side logout is sufficient
    return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
}
```

**Considerations**:
- For true stateless JWT, backend logout is optional (tokens expire naturally)
- If implementing token blacklist: requires Redis/cache to store revoked tokens until expiry
- Validate token before accepting logout request
- Return 200 OK even if token is already invalid (prevent information leakage)

### 1.2 Enhance Error Responses
**File**: `backend/src/main/java/com/stocktracker/config/SecurityConfig.java`

**Ensure Proper HTTP Status Codes**:
- `401 Unauthorized`: Invalid/expired/missing token
- `403 Forbidden`: Valid token but insufficient permissions
- Return consistent JSON error format matching `ApiResponse<T>` structure

---

## Phase 2: Frontend Core Logic

### 2.1 Enhance Auth Service
**File**: `frontend/src/services/authService.ts`

**Add Backend Logout Call** (if Phase 1.1 implemented):
```typescript
async logout(): Promise<void> {
  try {
    // Call backend logout endpoint
    await api.post('/auth/logout')
  } catch (error) {
    // Ignore errors - proceed with client-side cleanup
    console.warn('Backend logout failed, proceeding with local cleanup')
  } finally {
    // Always clear local storage
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }
}
```

**Alternative (Client-side only)**:
Keep existing synchronous `logout()` method if backend endpoint not implemented.

**Add Session Check Method**:
```typescript
checkSession(): boolean {
  const token = this.getToken()
  if (!token) return false

  // Optional: Decode JWT and check expiration
  // For now, just check existence
  return true
}
```

### 2.2 Create Logout Page Component
**File**: `frontend/src/pages/Logout/Logout.tsx`

**Features**:
- Display logout success message with icon
- Show "Redirecting to login..." with countdown timer (3 seconds)
- Button to immediately return to login page
- Button to return to home page
- Professional styling matching existing design system (TailwindCSS)

**User Flow**:
1. User clicks logout button → lands on `/logout` page
2. See confirmation message: "You have been successfully logged out"
3. Auto-redirect to `/login` after 3 seconds (with countdown)
4. Manual navigation options: "Return to Login" or "Go to Home"

**Styling**:
- Use existing design tokens (Slate, Indigo colors)
- Center-aligned card layout similar to Login page
- Success icon (checkmark in circle)
- Clear typography hierarchy

### 2.3 Update Axios Interceptor
**File**: `frontend/src/services/api/axiosInstance.ts`

**Current Implementation**:
```typescript
// Response interceptor
axiosInstance.interceptors.response.use(
  response => response,
  error => {
    // Only redirect to login on 401 if it's not the login endpoint itself
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      localStorage.removeItem('authToken')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
```

**Enhanced Implementation**:
```typescript
// Response interceptor
axiosInstance.interceptors.response.use(
  response => response,
  error => {
    const status = error.response?.status
    const url = error.config?.url

    // Handle 401 Unauthorized - Invalid/expired token
    if (status === 401 && !url?.includes('/auth/login')) {
      localStorage.removeItem('authToken')
      localStorage.removeItem('user')
      window.location.href = '/login'
      return Promise.reject(error)
    }

    // Handle 403 Forbidden - Insufficient permissions
    if (status === 403) {
      // Clear auth data and redirect to home page
      localStorage.removeItem('authToken')
      localStorage.removeItem('user')
      window.location.href = '/'
      return Promise.reject(error)
    }

    return Promise.reject(error)
  }
)
```

**Edge Cases**:
- Don't redirect on login endpoint 401 (user entered wrong credentials)
- Don't redirect on logout endpoint errors (already logging out)
- Clear localStorage before redirect to prevent stale data
- Use `window.location.href` for hard navigation (clears React state)

---

## Phase 3: Navigation Integration

### 3.1 Add User Dropdown Menu
**File**: `frontend/src/components/layout/DashboardNavigation.tsx`

**Current State**:
- User avatar icon exists (line 99-101)
- No dropdown menu functionality
- No logout button

**Implementation**:
Replace static user avatar with dropdown menu containing:
- User email/name display
- "Account Settings" option (placeholder for future)
- "Logout" button with icon

**Dropdown Behavior**:
- Click avatar to toggle dropdown
- Click outside to close dropdown
- ESC key to close dropdown
- Smooth animation (slide down/fade in)
- Proper z-index layering

**Logout Button Click Handler**:
```typescript
const handleLogout = async () => {
  await authService.logout() // If async backend call
  // Or: authService.logout() // If synchronous
  navigate('/logout')
}
```

**Accessibility**:
- ARIA labels for screen readers
- Keyboard navigation support (Tab, Enter, ESC)
- Focus management (return focus to avatar after close)

**Styling**:
- Match existing design system
- Dropdown positioned below avatar (right-aligned)
- Hover states for menu items
- Red/destructive color for logout button
- Icon + text for logout (logout icon from Heroicons)

### 3.2 Add Logout to Mobile Menu (if applicable)
If mobile navigation exists or planned:
- Add logout option to mobile hamburger menu
- Same styling and behavior as desktop

---

## Phase 4: Routing & Route Protection

### 4.1 Add Logout Route
**File**: `frontend/src/App.tsx`

**Current Routes**:
```tsx
<Routes>
  <Route path="/" element={<Home />} />
  <Route path="/login" element={<Login />} />
  <Route path="/dashboard" element={<Dashboard />} />
  <Route path="/portfolios" element={<Portfolios />} />
  <Route path="/watchlist" element={<Watchlist />} />
</Routes>
```

**Add Logout Route**:
```tsx
import { Home, Login, Dashboard, Portfolios, Watchlist, Logout } from '@pages/index'

<Routes>
  <Route path="/" element={<Home />} />
  <Route path="/login" element={<Login />} />
  <Route path="/logout" element={<Logout />} />
  <Route path="/dashboard" element={<Dashboard />} />
  <Route path="/portfolios" element={<Portfolios />} />
  <Route path="/watchlist" element={<Watchlist />} />
</Routes>
```

### 4.2 Implement Route Protection (Recommended)
**Create**: `frontend/src/components/auth/ProtectedRoute.tsx`

**Purpose**: Prevent unauthenticated users from accessing protected pages

**Implementation**:
```typescript
import { Navigate, useLocation } from 'react-router-dom'
import { authService } from '@services/authService'

interface ProtectedRouteProps {
  children: React.ReactNode
}

export const ProtectedRoute = ({ children }: ProtectedRouteProps) => {
  const location = useLocation()
  const isAuthenticated = authService.isAuthenticated()

  if (!isAuthenticated) {
    // Redirect to login, save attempted URL for post-login redirect
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return <>{children}</>
}
```

**Update Routes**:
```tsx
<Routes>
  <Route path="/" element={<Home />} />
  <Route path="/login" element={<Login />} />
  <Route path="/logout" element={<Logout />} />
  <Route path="/dashboard" element={
    <ProtectedRoute><Dashboard /></ProtectedRoute>
  } />
  <Route path="/portfolios" element={
    <ProtectedRoute><Portfolios /></ProtectedRoute>
  } />
  <Route path="/watchlist" element={
    <ProtectedRoute><Watchlist /></ProtectedRoute>
  } />
</Routes>
```

### 4.3 Enhance Login with Post-Login Redirect
**File**: `frontend/src/pages/Login/Login.tsx`

**Current Implementation**:
```typescript
await authService.login({ email, password })
navigate('/dashboard')
```

**Enhanced Implementation**:
```typescript
import { useLocation } from 'react-router-dom'

const Login = () => {
  const location = useLocation()
  // Get the page user tried to access before being redirected to login
  const from = location.state?.from?.pathname || '/dashboard'

  const handleSubmit = async (e: FormEvent) => {
    // ... validation ...

    try {
      await authService.login({ email, password })
      navigate(from, { replace: true }) // Redirect to original destination
    } catch (err) {
      // ... error handling ...
    }
  }
}
```

**User Experience**:
- User tries to access `/dashboard` while logged out
- Redirected to `/login`
- After successful login, taken to `/dashboard` (not home)

---

## Phase 5: User Experience Enhancements

### 5.1 Logout Confirmation Modal (Optional)
**Create**: `frontend/src/components/modals/LogoutConfirmModal.tsx`

**Purpose**: Prevent accidental logouts

**Features**:
- Modal dialog: "Are you sure you want to logout?"
- "Cancel" button (default/primary)
- "Logout" button (destructive/secondary)
- ESC key to cancel
- Click outside to cancel

**When to Use**:
- If user has unsaved work in the future
- For now, direct logout is acceptable (no data loss)

### 5.2 Session Expiry Warning (Future Enhancement)
**Purpose**: Warn user before token expires

**Implementation**:
- Decode JWT to get expiration time
- Show toast/modal 2 minutes before expiry
- "Extend Session" button (re-authenticate)
- "Logout Now" button

**Not Required for Phase 1**

---

## Phase 6: Testing Requirements

### 6.1 Manual Testing Checklist

**Logout Flow**:
- [ ] Click logout button in navigation
- [ ] Verify redirect to `/logout` page
- [ ] Verify success message displays
- [ ] Verify countdown timer works (3, 2, 1...)
- [ ] Verify auto-redirect to `/login` after 3 seconds
- [ ] Click "Return to Login" button manually
- [ ] Click "Go to Home" button manually
- [ ] Verify localStorage is cleared (check DevTools)

**Session Expiry - 401 Errors**:
- [ ] Log in successfully
- [ ] Manually expire token (edit in DevTools or wait for expiry)
- [ ] Make API request (e.g., refresh portfolio)
- [ ] Verify automatic redirect to `/login`
- [ ] Verify localStorage is cleared
- [ ] Verify error toast/message (optional)

**Forbidden Access - 403 Errors**:
- [ ] Trigger 403 error (modify user permissions in DB or mock response)
- [ ] Verify automatic redirect to `/` (home page)
- [ ] Verify localStorage is cleared
- [ ] Verify user sees appropriate message on home page

**Route Protection**:
- [ ] Log out completely
- [ ] Manually navigate to `/dashboard` (URL bar)
- [ ] Verify redirect to `/login`
- [ ] Log in successfully
- [ ] Verify redirect back to `/dashboard`

**Edge Cases**:
- [ ] Logout while network is offline (should still clear localStorage)
- [ ] Multiple rapid logout clicks (should not cause errors)
- [ ] Logout from multiple tabs (test cross-tab behavior)
- [ ] Back button after logout (should not access protected pages)

### 6.2 Unit Testing (Recommended)

**authService.test.ts**:
- Test `logout()` clears localStorage
- Test `isAuthenticated()` returns correct boolean
- Test `getToken()` retrieves correct token

**Logout.test.tsx**:
- Test component renders success message
- Test countdown timer decrements
- Test auto-redirect after countdown
- Test manual navigation buttons

**ProtectedRoute.test.tsx**:
- Test redirects unauthenticated users
- Test allows authenticated users
- Test preserves intended destination

### 6.3 Integration Testing (Advanced)

**E2E Test Flow** (Playwright/Cypress):
```
1. Navigate to login page
2. Enter valid credentials
3. Click login button
4. Verify redirect to dashboard
5. Click user avatar
6. Click logout button
7. Verify redirect to logout page
8. Wait for auto-redirect to login
9. Verify cannot access dashboard without login
```

---

## Phase 7: Security Considerations

### 7.1 Token Management
- **Never log tokens**: Ensure tokens are not logged to console or analytics
- **Clear on logout**: Always clear both `authToken` and `user` data
- **HTTP-only cookies (future)**: Consider moving to HTTP-only cookies instead of localStorage for better XSS protection

### 7.2 Logout Endpoint Security
- **Rate limiting**: Prevent logout spam/DoS (backend)
- **Token validation**: Verify token before accepting logout request
- **No information leakage**: Return 200 OK even for invalid tokens

### 7.3 Client-Side Security
- **No sensitive data in state**: Clear all Redux/Context state on logout
- **Clear browser cache**: Consider adding `Cache-Control` headers for logout page
- **Prevent back-button access**: Use `replace: true` in navigation after logout

---

## Phase 8: Implementation Order

### Recommended Sequence:
1. **Phase 2.2**: Create Logout page component (visible deliverable)
2. **Phase 4.1**: Add `/logout` route to App.tsx
3. **Phase 2.1**: Enhance authService (if adding backend call)
4. **Phase 3.1**: Add logout button to DashboardNavigation
5. **Phase 2.3**: Update Axios interceptor (403 handling)
6. **Phase 4.2**: Implement ProtectedRoute component
7. **Phase 4.3**: Update Login with post-login redirect
8. **Phase 6.1**: Manual testing
9. **Phase 1** (Optional): Backend logout endpoint
10. **Phase 6.2-6.3**: Automated testing

---

## Phase 9: Visual Design Specifications

### Logout Page Layout
```
┌─────────────────────────────────────┐
│                                     │
│           [Success Icon]            │
│                                     │
│       You've been logged out        │
│                                     │
│  Your session has ended securely.   │
│  Redirecting to login in 3s...      │
│                                     │
│     [Return to Login]  [Home]       │
│                                     │
└─────────────────────────────────────┘
```

### Color Scheme (TailwindCSS)
- **Background**: `bg-slate-50`
- **Card**: `bg-white` with `border-slate-200`
- **Success Icon**: `text-emerald-500` (checkmark in circle)
- **Heading**: `text-slate-900` font-bold text-2xl
- **Body Text**: `text-slate-600` text-sm
- **Primary Button**: `bg-indigo-600 hover:bg-indigo-700 text-white`
- **Secondary Button**: `bg-slate-100 hover:bg-slate-200 text-slate-700`

### User Dropdown Menu Layout
```
┌─────────────────────────┐
│ U  user@example.com     │
│ ─────────────────────── │
│ ⚙  Account Settings     │
│ 🚪 Logout               │
└─────────────────────────┘
```

### Spacing & Sizing
- **Card Width**: `max-w-md` (28rem)
- **Card Padding**: `p-8`
- **Icon Size**: `w-16 h-16`
- **Button Height**: `h-11` (44px)
- **Gap Between Elements**: `gap-6`

---

## Phase 10: Error Handling & Edge Cases

### Network Errors During Logout
**Scenario**: User clicks logout but backend is unreachable

**Handling**:
```typescript
async logout(): Promise<void> {
  try {
    await api.post('/auth/logout', {}, { timeout: 3000 })
  } catch (error) {
    // Log error for debugging but don't block logout
    console.warn('Logout request failed:', error)
  } finally {
    // ALWAYS clear local data regardless of backend response
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }
}
```

### Multiple Tabs/Windows
**Scenario**: User has multiple tabs open, logs out from one

**Current Behavior**: Other tabs remain "logged in" locally

**Solution (Future Enhancement)**:
- Listen to `storage` event in React
- Detect when `authToken` is removed from localStorage
- Auto-redirect other tabs to logout page

**Implementation**:
```typescript
useEffect(() => {
  const handleStorageChange = (e: StorageEvent) => {
    if (e.key === 'authToken' && e.newValue === null) {
      // Token was removed, user logged out in another tab
      navigate('/logout')
    }
  }

  window.addEventListener('storage', handleStorageChange)
  return () => window.removeEventListener('storage', handleStorageChange)
}, [navigate])
```

### Back Button After Logout
**Scenario**: User logs out, presses back button

**Expected**: Cannot access protected pages

**Solution**: Already handled by ProtectedRoute component (Phase 4.2)

### Logout During Active Operations
**Scenario**: User has pending API requests when they logout

**Solution**: Axios will cancel pending requests or they'll fail silently

**Enhancement** (Optional):
- Cancel all pending requests on logout
- Use AbortController with axios

---

## Phase 11: Accessibility (a11y)

### Keyboard Navigation
- **Tab order**: Avatar → Dropdown items → Buttons
- **Enter/Space**: Activate buttons and toggle dropdown
- **ESC**: Close dropdown menu
- **Focus trap**: Keep focus within dropdown when open

### Screen Reader Support
```tsx
// User Avatar Button
<button
  aria-label="User menu"
  aria-expanded={isDropdownOpen}
  aria-haspopup="true"
>
  {/* ... */}
</button>

// Dropdown Menu
<div
  role="menu"
  aria-label="User menu options"
>
  <button role="menuitem">Account Settings</button>
  <button role="menuitem">Logout</button>
</div>

// Logout Page
<div role="main" aria-labelledby="logout-heading">
  <h1 id="logout-heading">You've been logged out</h1>
  {/* ... */}
</div>
```

### Focus Management
- When dropdown opens: focus first menu item
- When dropdown closes: return focus to avatar button
- When logout page loads: focus on heading
- Visible focus indicators for all interactive elements

---

## Phase 12: Analytics & Monitoring (Optional)

### Events to Track
- `user_logout_initiated` - User clicked logout button
- `user_logout_completed` - Reached logout page
- `session_expired_401` - Automatic logout due to 401
- `access_forbidden_403` - Automatic redirect due to 403
- `logout_backend_failed` - Backend logout endpoint failed

### Error Monitoring
- Log 403 errors with context (endpoint, user ID)
- Log backend logout failures
- Track logout completion rate

---

## Files to Create

### New Files
1. `frontend/src/pages/Logout/Logout.tsx` - Logout success page
2. `frontend/src/pages/Logout/Logout.module.css` - Styles (if using CSS modules)
3. `frontend/src/components/auth/ProtectedRoute.tsx` - Route guard component
4. `frontend/src/components/layout/UserDropdown.tsx` - User menu dropdown (optional, can be in DashboardNavigation)

### Files to Modify
1. `frontend/src/App.tsx` - Add logout route
2. `frontend/src/services/authService.ts` - Enhance logout method
3. `frontend/src/services/api/axiosInstance.ts` - Add 403 handling
4. `frontend/src/components/layout/DashboardNavigation.tsx` - Add logout button/dropdown
5. `frontend/src/pages/Login/Login.tsx` - Add post-login redirect
6. `frontend/src/pages/index.ts` - Export Logout component
7. `backend/src/main/java/com/stocktracker/controller/AuthController.java` - Add logout endpoint (optional)

---

## Success Criteria

### Functional Requirements
✅ User can logout from navigation menu
✅ Logout clears all authentication data from localStorage
✅ Logout page displays success message
✅ Auto-redirect to login after 3 seconds
✅ Manual navigation buttons work (Login, Home)
✅ 401 errors redirect to `/login` with cleared session
✅ 403 errors redirect to `/` (Home) with cleared session
✅ Protected routes require authentication
✅ Post-login redirect to intended destination

### Non-Functional Requirements
✅ Responsive design (mobile, tablet, desktop)
✅ Accessible (WCAG 2.1 Level AA)
✅ Smooth animations (no janky transitions)
✅ Error handling (network failures don't break logout)
✅ Cross-browser compatibility (Chrome, Firefox, Safari, Edge)
✅ Secure (no token leakage, proper cleanup)

---

## Future Enhancements

### Not in Scope for Initial Implementation
- Logout confirmation modal (add if users report accidental logouts)
- Session expiry warning (add when implementing token refresh)
- Cross-tab logout sync (add if multi-tab usage is common)
- Remember me / Keep me logged in (separate feature)
- Logout from all devices (requires backend session management)
- Activity timeout (auto-logout after inactivity)

---

## Questions for Clarification

Before implementation, confirm:
1. **Backend logout endpoint**: Implement or skip? (Recommend: skip for now, add later if needed)
2. **Logout confirmation**: Show modal or direct logout? (Recommend: direct for now)
3. **Post-logout destination**: Always `/login` or sometimes `/` (home)? (Recommend: `/login`)
4. **Route protection**: Protect all authenticated routes or only some? (Recommend: all)
5. **403 error message**: Show error toast before redirecting to home? (Recommend: yes, brief toast)
