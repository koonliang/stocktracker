import type { Config } from 'tailwindcss'
import tailwindcssFormsPlugin from '@tailwindcss/forms'

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
        sans: [
          'var(--font-plus-jakarta-sans)',
          'Plus Jakarta Sans',
          'system-ui',
          '-apple-system',
          'BlinkMacSystemFont',
          'Segoe UI',
          'Roboto',
          'sans-serif',
        ],
      },
      boxShadow: {
        soft: '0 4px 20px -2px rgba(79, 70, 229, 0.1)',
        'soft-hover':
          '0 10px 25px -5px rgba(79, 70, 229, 0.15), 0 8px 10px -6px rgba(79, 70, 229, 0.1)',
        button: '0 4px 14px 0 rgba(79, 70, 229, 0.3)',
      },
      animation: {
        'pulse-slow': 'pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        fadeIn: 'fadeIn 0.15s ease-in-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(-4px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [tailwindcssFormsPlugin],
}

export default config
