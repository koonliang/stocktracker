import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: ['class', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        bg: 'rgb(var(--color-bg) / <alpha-value>)',
        surface: 'rgb(var(--color-surface) / <alpha-value>)',
        'surface-alt': 'rgb(var(--color-surface-alt) / <alpha-value>)',
        border: 'rgb(var(--color-border) / <alpha-value>)',
        'border-strong': 'rgb(var(--color-border-strong) / <alpha-value>)',
        text: 'rgb(var(--color-text) / <alpha-value>)',
        'text-muted': 'rgb(var(--color-text-muted) / <alpha-value>)',
        'text-subtle': 'rgb(var(--color-text-subtle) / <alpha-value>)',
        accent: 'rgb(var(--color-accent) / <alpha-value>)',
        'accent-hover': 'rgb(var(--color-accent-hover) / <alpha-value>)',
        'accent-fg': 'rgb(var(--color-accent-fg) / <alpha-value>)',
        positive: 'rgb(var(--color-positive) / <alpha-value>)',
        'positive-bg': 'rgb(var(--color-positive-bg) / <alpha-value>)',
        negative: 'rgb(var(--color-negative) / <alpha-value>)',
        'negative-bg': 'rgb(var(--color-negative-bg) / <alpha-value>)',
        warning: 'rgb(var(--color-warning) / <alpha-value>)',
        'focus-ring': 'rgb(var(--color-focus-ring) / <alpha-value>)',
      },
      fontFamily: {
        sans: ['"IBM Plex Sans"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace'],
        display: ['"IBM Plex Sans"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        'display-lg': [
          'clamp(1.75rem, 2.2vw + 1rem, 2.5rem)',
          { lineHeight: '1.1', letterSpacing: '-0.02em', fontWeight: '600' },
        ],
        display: [
          'clamp(1.625rem, 1.6vw + 1rem, 2rem)',
          { lineHeight: '1.15', letterSpacing: '-0.02em', fontWeight: '600' },
        ],
        headline: ['1.375rem', { lineHeight: '1.2', letterSpacing: '-0.01em', fontWeight: '600' }],
        title: ['1.0625rem', { lineHeight: '1.35', fontWeight: '600' }],
        body: ['0.9375rem', { lineHeight: '1.55' }],
        small: ['0.8125rem', { lineHeight: '1.5' }],
        micro: ['0.6875rem', { lineHeight: '1.4', letterSpacing: '0.08em' }],
      },
      spacing: {
        '4.5': '1.125rem',
        '18': '4.5rem',
        '22': '5.5rem',
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        DEFAULT: 'var(--radius-md)',
        md: 'var(--radius-md)',
        lg: 'var(--radius-lg)',
        xl: 'var(--radius-xl)',
        '2xl': 'var(--radius-2xl)',
      },
      boxShadow: {
        card: 'var(--shadow-card)',
        popover: 'var(--shadow-popover)',
        focus: '0 0 0 3px rgb(var(--color-focus-ring) / 0.4)',
      },
      transitionTimingFunction: {
        'out-expo': 'cubic-bezier(0.22, 1, 0.36, 1)',
      },
    },
  },
  plugins: [],
};

export default config;
