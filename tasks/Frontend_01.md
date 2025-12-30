# Task 01: Generate Initial Frontend Framework

## Overview

Set up the initial frontend framework for the Stock Tracker application using React with Vite as the build tool. This task establishes the foundation for all frontend development, including project structure, essential configurations, and development tooling.

---

## Objectives

1. Initialize a new React project using Vite
2. Configure TypeScript for type-safe development
3. Set up the project folder structure following best practices
4. Install and configure essential dependencies
5. Configure development environment and tooling
6. Prepare the foundation for routing and state management

---

## Technical Specifications

### Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| React | ^18.x | UI Library |
| Vite | ^5.x | Build Tool & Dev Server |
| TypeScript | ^5.x | Type Safety |
| React Router | ^6.x | Client-side Routing |
| Axios | ^1.x | HTTP Client |
| ESLint | ^8.x | Code Linting |
| Prettier | ^3.x | Code Formatting |

### System Requirements

- Node.js v18.x or higher
- npm v9.x or higher (or yarn/pnpm)
- Git for version control

---

## Implementation Steps

### Step 1: Initialize Vite React Project

Navigate to the frontend directory and create a new Vite project with React and TypeScript template:

```bash
cd frontend
npm create vite@latest . -- --template react-ts
```

If the directory already has files, you may need to clear it first or use a temporary directory and move files.

### Step 2: Install Core Dependencies

```bash
npm install
```

### Step 3: Install Additional Dependencies

**Runtime Dependencies:**
```bash
npm install react-router-dom axios
```

**Development Dependencies:**
```bash
npm install -D @types/node eslint eslint-plugin-react eslint-plugin-react-hooks @typescript-eslint/eslint-plugin @typescript-eslint/parser prettier eslint-config-prettier eslint-plugin-prettier
```

### Step 4: Configure TypeScript

Update `tsconfig.json` with the following configuration:

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"],
      "@components/*": ["src/components/*"],
      "@pages/*": ["src/pages/*"],
      "@services/*": ["src/services/*"],
      "@hooks/*": ["src/hooks/*"],
      "@utils/*": ["src/utils/*"],
      "@types/*": ["src/types/*"],
      "@assets/*": ["src/assets/*"]
    }
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

### Step 5: Configure Vite

Update `vite.config.ts`:

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@components': path.resolve(__dirname, './src/components'),
      '@pages': path.resolve(__dirname, './src/pages'),
      '@services': path.resolve(__dirname, './src/services'),
      '@hooks': path.resolve(__dirname, './src/hooks'),
      '@utils': path.resolve(__dirname, './src/utils'),
      '@types': path.resolve(__dirname, './src/types'),
      '@assets': path.resolve(__dirname, './src/assets'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

### Step 6: Configure ESLint

Create `.eslintrc.cjs`:

```javascript
module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react-hooks/recommended',
    'plugin:react/recommended',
    'plugin:prettier/recommended',
  ],
  ignorePatterns: ['dist', '.eslintrc.cjs'],
  parser: '@typescript-eslint/parser',
  plugins: ['react-refresh'],
  settings: {
    react: {
      version: 'detect',
    },
  },
  rules: {
    'react-refresh/only-export-components': [
      'warn',
      { allowConstantExport: true },
    ],
    'react/react-in-jsx-scope': 'off',
  },
}
```

### Step 7: Configure Prettier

Create `.prettierrc`:

```json
{
  "semi": false,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100,
  "bracketSpacing": true,
  "jsxSingleQuote": false,
  "arrowParens": "avoid"
}
```

Create `.prettierignore`:

```
node_modules
dist
build
coverage
```

### Step 8: Create Project Folder Structure

```
frontend/
├── public/
│   └── favicon.ico
├── src/
│   ├── assets/
│   │   ├── images/
│   │   └── styles/
│   │       └── global.css
│   ├── components/
│   │   ├── common/
│   │   │   ├── Button/
│   │   │   ├── Input/
│   │   │   └── index.ts
│   │   ├── layout/
│   │   │   ├── Header/
│   │   │   ├── Footer/
│   │   │   ├── Sidebar/
│   │   │   └── index.ts
│   │   └── index.ts
│   ├── hooks/
│   │   └── index.ts
│   ├── pages/
│   │   ├── Login/
│   │   │   ├── Login.tsx
│   │   │   ├── Login.module.css
│   │   │   └── index.ts
│   │   ├── Dashboard/
│   │   │   ├── Dashboard.tsx
│   │   │   ├── Dashboard.module.css
│   │   │   └── index.ts
│   │   └── index.ts
│   ├── services/
│   │   ├── api/
│   │   │   ├── axiosInstance.ts
│   │   │   └── index.ts
│   │   └── index.ts
│   ├── types/
│   │   └── index.ts
│   ├── utils/
│   │   └── index.ts
│   ├── App.tsx
│   ├── main.tsx
│   └── vite-env.d.ts
├── .eslintrc.cjs
├── .prettierrc
├── .prettierignore
├── .gitignore
├── index.html
├── package.json
├── tsconfig.json
├── tsconfig.node.json
└── vite.config.ts
```

### Step 9: Create Base Axios Instance

Create `src/services/api/axiosInstance.ts`:

```typescript
import axios from 'axios'

const axiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor
axiosInstance.interceptors.request.use(
  config => {
    const token = localStorage.getItem('authToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Response interceptor
axiosInstance.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('authToken')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default axiosInstance
```

### Step 10: Set Up Basic Routing

Update `src/App.tsx`:

```typescript
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<div>Login Page</div>} />
        <Route path="/dashboard" element={<div>Dashboard Page</div>} />
        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
    </Router>
  )
}

export default App
```

### Step 11: Update Package.json Scripts

Ensure `package.json` has the following scripts:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "lint:fix": "eslint . --ext ts,tsx --fix",
    "format": "prettier --write \"src/**/*.{ts,tsx,css,json}\""
  }
}
```

---

## Environment Configuration

Create `.env` file for environment variables:

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=Stock Tracker
```

Create `.env.example` as a template:

```env
VITE_API_BASE_URL=
VITE_APP_NAME=
```

---

## Verification Steps

After completing the implementation, verify the setup by:

1. **Start Development Server:**
   ```bash
   npm run dev
   ```
   The application should be accessible at `http://localhost:3000`

2. **Run Linting:**
   ```bash
   npm run lint
   ```
   Should complete with no errors

3. **Run Build:**
   ```bash
   npm run build
   ```
   Should complete successfully and generate `dist` folder

4. **Test Path Aliases:**
   Import using path aliases and verify they resolve correctly

---

## Acceptance Criteria

- [ ] Vite + React + TypeScript project is initialized
- [ ] All dependencies are installed without errors
- [ ] TypeScript configuration is properly set up with path aliases
- [ ] ESLint and Prettier are configured and working
- [ ] Project folder structure is created as specified
- [ ] Axios instance is configured with interceptors
- [ ] React Router is set up with basic routes
- [ ] Development server starts without errors
- [ ] Build completes successfully
- [ ] Proxy is configured to forward `/api` requests to backend

---

## Dependencies Summary

### Runtime Dependencies
- `react` - UI library
- `react-dom` - React DOM renderer
- `react-router-dom` - Routing library
- `axios` - HTTP client

### Development Dependencies
- `typescript` - Type safety
- `vite` - Build tool
- `@vitejs/plugin-react` - Vite React plugin
- `eslint` - Linting
- `prettier` - Code formatting
- `@typescript-eslint/*` - TypeScript ESLint support
- `eslint-plugin-react` - React ESLint rules
- `eslint-plugin-react-hooks` - React Hooks ESLint rules

---

## Notes

1. **Port Configuration:** Frontend runs on port 3000, backend on port 8080. Vite proxy is configured to forward API requests.

2. **Authentication:** The axios instance includes token handling - tokens are expected to be stored in localStorage under `authToken`.

3. **Styling:** This setup uses CSS Modules. Consider adding Tailwind CSS or a UI component library (MUI, Ant Design) based on project requirements.

4. **State Management:** For complex state, consider adding Zustand, Redux Toolkit, or React Query in future tasks.

---

## Related Tasks

- **Task 02:** Generate initial Java Spring Boot backend
- **Task 03:** Create a simple login page with email and password
- **Task 05:** Create a simple dashboard page
