# Task 03: Create Simple Login Page

## Overview

Create a functional login page for the Stock Tracker application with email and password authentication. This task implements the frontend login form that integrates with the Spring Boot backend `/api/auth/login` endpoint, handles JWT token storage, and manages user authentication state.

---

## Objectives

1. Create TypeScript interfaces for authentication types
2. Implement authentication service for API communication
3. Build a responsive login form with email and password fields
4. Add form validation (client-side)
5. Handle API errors and display user-friendly messages
6. Store JWT token and redirect to dashboard on successful login
7. Style the login page following the Corporate Trust design system

---

## Technical Specifications

### Technology Stack

| Technology | Purpose |
|------------|---------|
| React 19.x | UI Framework |
| TypeScript 5.x | Type Safety |
| React Router 7.x | Navigation/Redirect |
| Axios | HTTP Client |
| CSS Modules | Component Styling |

### Design System: Corporate Trust

This login page follows the **Corporate Trust** design system — a modern enterprise SaaS aesthetic that is professional yet approachable.

| Token | Value | Usage |
|-------|-------|-------|
| **Background** | `#F8FAFC` (Slate 50) | Page background |
| **Surface** | `#FFFFFF` | Card background |
| **Primary** | `#4F46E5` (Indigo 600) | Buttons, focus states |
| **Secondary** | `#7C3AED` (Violet 600) | Gradients, accents |
| **Text Main** | `#0F172A` (Slate 900) | Headlines, labels |
| **Text Muted** | `#64748B` (Slate 500) | Subtitles, placeholders |
| **Border** | `#E2E8F0` (Slate 200) | Input borders |
| **Error** | `#EF4444` (Red 500) | Error messages |
| **Font** | Plus Jakarta Sans | All text |
| **Card Radius** | `12px` (rounded-xl) | Card corners |
| **Input Radius** | `8px` (rounded-lg) | Input corners |
| **Shadow** | Indigo-tinted | Colored shadows |

### Backend Integration

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/login` | POST | Authenticate user |

### Request/Response Format

**Login Request:**
```json
{
  "email": "demo@stocktracker.com",
  "password": "password123"
}
```

**Success Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "userId": 1,
    "email": "demo@stocktracker.com",
    "name": "Demo User"
  }
}
```

**Error Response:**
```json
{
  "success": false,
  "message": "Invalid email or password"
}
```

---

## Implementation Steps

### Step 1: Create Authentication Types

**File:** `src/types/auth.ts`

```typescript
export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  type: string
  userId: number
  email: string
  name: string
}

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
}

export interface User {
  id: number
  email: string
  name: string
}
```

### Step 2: Create Authentication Service

**File:** `src/services/authService.ts`

```typescript
import api from './api'
import type { LoginRequest, AuthResponse, ApiResponse } from '@/types/auth'

const TOKEN_KEY = 'authToken'
const USER_KEY = 'user'

export const authService = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<ApiResponse<AuthResponse>>(
      '/auth/login',
      credentials
    )

    if (response.data.success && response.data.data) {
      const authData = response.data.data
      localStorage.setItem(TOKEN_KEY, authData.token)
      localStorage.setItem(USER_KEY, JSON.stringify({
        id: authData.userId,
        email: authData.email,
        name: authData.name,
      }))
      return authData
    }

    throw new Error(response.data.message || 'Login failed')
  },

  logout(): void {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  },

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY)
  },

  getUser(): { id: number; email: string; name: string } | null {
    const user = localStorage.getItem(USER_KEY)
    return user ? JSON.parse(user) : null
  },

  isAuthenticated(): boolean {
    return !!this.getToken()
  },
}

export default authService
```

### Step 3: Update Types Index

**File:** `src/types/index.ts`

```typescript
export * from './auth'
```

### Step 4: Update Services Index

**File:** `src/services/index.ts`

```typescript
export { default as api } from './api'
export { authService } from './authService'
```

### Step 5: Create Login Page Component

**File:** `src/pages/Login/Login.tsx`

```typescript
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { authService } from '@services/authService'
import styles from './Login.module.css'

const Login = () => {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')

    // Basic validation
    if (!email || !password) {
      setError('Please enter both email and password')
      return
    }

    if (!email.includes('@')) {
      setError('Please enter a valid email address')
      return
    }

    setIsLoading(true)

    try {
      await authService.login({ email, password })
      navigate('/dashboard')
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message)
      } else {
        setError('An unexpected error occurred. Please try again.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>Stock Tracker</h1>
        <p className={styles.subtitle}>Sign in to your account</p>

        <form onSubmit={handleSubmit} className={styles.form}>
          {error && <div className={styles.error}>{error}</div>}

          <div className={styles.field}>
            <label htmlFor="email" className={styles.label}>
              Email
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className={styles.input}
              placeholder="Enter your email"
              disabled={isLoading}
              autoComplete="email"
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="password" className={styles.label}>
              Password
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              className={styles.input}
              placeholder="Enter your password"
              disabled={isLoading}
              autoComplete="current-password"
            />
          </div>

          <button
            type="submit"
            className={styles.button}
            disabled={isLoading}
          >
            {isLoading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default Login
```

### Step 6: Create Login Styles

**File:** `src/pages/Login/Login.module.css`

Following the **Corporate Trust** design system with light mode, indigo/violet palette, and colored shadows.

```css
/* Import Plus Jakarta Sans font */
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: #F8FAFC;
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  position: relative;
  overflow: hidden;
}

/* Atmospheric background blur orbs */
.container::before {
  content: '';
  position: absolute;
  top: -200px;
  right: -200px;
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.15;
  pointer-events: none;
}

.container::after {
  content: '';
  position: absolute;
  bottom: -150px;
  left: -150px;
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #7C3AED 0%, #4F46E5 100%);
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.1;
  pointer-events: none;
}

.card {
  background: #FFFFFF;
  border-radius: 12px;
  padding: 2.5rem;
  width: 100%;
  max-width: 400px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 4px 20px -2px rgba(79, 70, 229, 0.1);
  transition: transform 0.2s ease-out, box-shadow 0.2s ease-out;
  position: relative;
  z-index: 1;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px -5px rgba(79, 70, 229, 0.15),
              0 8px 10px -6px rgba(79, 70, 229, 0.1);
}

.title {
  font-size: 1.75rem;
  font-weight: 800;
  text-align: center;
  margin: 0 0 0.5rem;
  color: #0F172A;
  letter-spacing: -0.02em;
  line-height: 1.1;
}

/* Gradient text effect for title */
.titleGradient {
  background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 0.875rem;
  text-align: center;
  color: #64748B;
  margin: 0 0 2rem;
  font-weight: 400;
  line-height: 1.6;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #334155;
}

.input {
  padding: 0.75rem 1rem;
  font-size: 1rem;
  font-family: inherit;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  background: #FFFFFF;
  color: #0F172A;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input:focus {
  outline: none;
  border-color: #4F46E5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
}

.input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #F8FAFC;
}

.input::placeholder {
  color: #94A3B8;
}

.button {
  margin-top: 0.5rem;
  padding: 0.875rem 1.5rem;
  font-size: 1rem;
  font-weight: 600;
  font-family: inherit;
  color: #FFFFFF;
  background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 4px 14px 0 rgba(79, 70, 229, 0.3);
  transition: transform 0.2s ease-out, box-shadow 0.2s ease-out;
}

.button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(79, 70, 229, 0.4);
}

.button:active:not(:disabled) {
  transform: translateY(0);
}

.button:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.4);
}

.button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #DC2626;
  background: #FEF2F2;
  border: 1px solid #FECACA;
  border-radius: 8px;
}

/* Loading spinner animation */
.spinner {
  display: inline-block;
  width: 1rem;
  height: 1rem;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #FFFFFF;
  animation: spin 0.8s linear infinite;
  margin-right: 0.5rem;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Responsive adjustments */
@media (max-width: 480px) {
  .container {
    padding: 1rem;
  }

  .card {
    padding: 1.5rem;
  }

  .title {
    font-size: 1.5rem;
  }
}
```

---

## Project Structure After Implementation

```
frontend/src/
├── pages/
│   └── Login/
│       ├── Login.tsx          # Login component (modified)
│       ├── Login.module.css   # Login styles (new)
│       └── index.ts           # Barrel export (existing)
├── services/
│   ├── api/
│   │   └── axiosInstance.ts   # Axios config (existing)
│   ├── authService.ts         # Auth service (new)
│   └── index.ts               # Services export (modified)
├── types/
│   ├── auth.ts                # Auth types (new)
│   └── index.ts               # Types export (modified)
└── ...
```

---

## Verification Steps

After completing the implementation, verify the setup by:

1. **Start Frontend Development Server:**
   ```bash
   cd frontend
   npm run dev
   ```
   Application should be accessible at `http://localhost:3000`

2. **Start Backend Server:**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   Backend should be running at `http://localhost:8080`

3. **Test Login with Demo Credentials:**
   - Navigate to `http://localhost:3000/login`
   - Enter email: `demo@stocktracker.com`
   - Enter password: `password123`
   - Click "Sign In"
   - Should redirect to `/dashboard`

4. **Test Error Handling:**
   - Enter invalid credentials
   - Should display "Invalid email or password" error

5. **Test Validation:**
   - Submit empty form
   - Should display validation error
   - Enter invalid email format
   - Should display email validation error

6. **Verify Token Storage:**
   - After successful login, open browser DevTools
   - Check localStorage for `authToken` and `user` keys

---

## Acceptance Criteria

- [ ] Login page displays centered card with form
- [ ] Email input field with label and placeholder
- [ ] Password input field with label and placeholder
- [ ] Submit button with loading state and gradient background
- [ ] Form validation for empty fields
- [ ] Form validation for email format
- [ ] Error messages display correctly (red on light background)
- [ ] Successful login stores JWT token in localStorage
- [ ] Successful login redirects to /dashboard
- [ ] Loading state disables form during API call
- [ ] **Design System Compliance:**
  - [ ] Uses Plus Jakarta Sans font
  - [ ] Light background (#F8FAFC) with white card
  - [ ] Indigo/Violet gradient button
  - [ ] Indigo-tinted colored shadows
  - [ ] Card lifts on hover with enhanced shadow
  - [ ] Indigo focus ring on inputs
  - [ ] Atmospheric blur orbs in background
- [ ] Form is accessible (labels, autocomplete attributes, focus-visible states)

---

## Test Credentials

| User | Email | Password |
|------|-------|----------|
| Demo User | demo@stocktracker.com | password123 |
| Admin User | admin@stocktracker.com | admin123 |

---

## Security Considerations

1. **Password Field:** Uses `type="password"` to mask input
2. **Autocomplete:** Proper autocomplete attributes for browser support
3. **Token Storage:** JWT stored in localStorage (consider httpOnly cookies for production)
4. **Error Messages:** Generic errors to prevent user enumeration
5. **HTTPS:** Ensure HTTPS in production for credential transmission

---

## Related Tasks

- **Task 01:** Generate initial frontend framework (Completed)
- **Task 02:** Generate initial Java Spring Boot backend (Completed)
- **Task 04:** Seed database with demo user (Covered in Task 02)
- **Task 05:** Create a simple dashboard page
