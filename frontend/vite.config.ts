/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      '@tests': path.resolve(__dirname, 'tests/support'),
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./tests/support/setup.ts'],
    css: true,
    include: ['tests/**/*.test.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'json-summary'],
      reportsDirectory: './coverage',
      clean: true,
      thresholds: {
        lines: 70,
        functions: 70,
        branches: 70,
        statements: 70,
      },
      exclude: [
        '**/coverage/**',
        '**/dist/**',
        '**/scripts/**',
        '**/tests/support/**',
        '**/tests/**',
        '**/*.d.ts',
        '**/src/main.tsx',
        '**/vite.config.ts',
        '**/postcss.config.js',
        '**/tailwind.config.ts',
        '**/.eslintrc.cjs',
      ],
    },
  },
});
