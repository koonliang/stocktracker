/**
 * Pure, locale-aware formatters. All numbers use en-US to keep the prototype
 * presentation consistent; localization is out of scope for this iteration.
 */

const nf = (options: Intl.NumberFormatOptions) => new Intl.NumberFormat('en-US', options);

const currencyFmt = nf({ style: 'currency', currency: 'USD', maximumFractionDigits: 2 });
const currencyFmtNoCents = nf({ style: 'currency', currency: 'USD', maximumFractionDigits: 0 });
const percentFmt = nf({ style: 'percent', minimumFractionDigits: 2, maximumFractionDigits: 2 });
const signedPercentFmt = nf({
  style: 'percent',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
  signDisplay: 'exceptZero',
});
const compactFmt = nf({ notation: 'compact', maximumFractionDigits: 2 });

export function formatCurrency(n: number | null | undefined, opts?: { cents?: boolean }): string {
  if (n == null || !Number.isFinite(n)) return '—';
  return (opts?.cents === false ? currencyFmtNoCents : currencyFmt).format(n);
}

export function formatSignedCurrency(
  n: number | null | undefined,
  opts?: { cents?: boolean },
): string {
  if (n == null || !Number.isFinite(n)) return '—';
  const sign = n > 0 ? '+' : n < 0 ? '−' : '';
  const fmt = opts?.cents === false ? currencyFmtNoCents : currencyFmt;
  return `${sign}${fmt.format(Math.abs(n))}`;
}

export function formatPercent(n: number | null | undefined): string {
  if (n == null || !Number.isFinite(n)) return '—';
  return percentFmt.format(n);
}

export function formatSignedPercent(n: number | null | undefined): string {
  if (n == null || !Number.isFinite(n)) return '—';
  // Intl doesn't render a proper minus for negative signed-percent in all locales —
  // normalize to typographic "−" so it matches our signed-currency output.
  return signedPercentFmt.format(n).replace('-', '−');
}

// Explicit symbols for currencies whose narrowSymbol in en-US ICU data resolves
// to a generic "$" (ambiguous with USD). Currencies with unambiguous narrowSymbol
// (€, £, ¥, ₹, ₩, …) are handled by Intl directly.
const CURRENCY_SYMBOLS: Record<string, string> = {
  USD: '$',
  AUD: 'A$',
  CAD: 'C$',
  HKD: 'HK$',
  MXN: 'MX$',
  NZD: 'NZ$',
  SGD: 'S$',
  TWD: 'NT$',
};

export function formatCurrencyCode(
  n: number | null | undefined,
  currency: string | null | undefined,
  opts?: { cents?: boolean },
): string {
  if (n == null || !Number.isFinite(n)) return '—';
  if (!currency) return formatCurrency(n, opts);
  const decimals = opts?.cents === false ? 0 : 2;
  const sym = CURRENCY_SYMBOLS[currency.toUpperCase()];
  if (sym) {
    return `${sym}${nf({ minimumFractionDigits: decimals, maximumFractionDigits: decimals }).format(n)}`;
  }
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    currencyDisplay: 'narrowSymbol',
    maximumFractionDigits: decimals,
  }).format(n);
}

export function formatSignedCurrencyCode(
  n: number | null | undefined,
  currency: string | null | undefined,
  opts?: { cents?: boolean },
): string {
  if (n == null || !Number.isFinite(n)) return '—';
  const sign = n > 0 ? '+' : n < 0 ? '−' : '';
  return `${sign}${formatCurrencyCode(Math.abs(n), currency, opts)}`;
}

export function formatCompactCurrencyCode(
  n: number | null | undefined,
  currency: string | null | undefined,
): string {
  if (n == null || !Number.isFinite(n)) return '—';
  const rounded = nf({ notation: 'compact', maximumFractionDigits: 1 }).format(n);
  if (!currency) return rounded;
  const sym = CURRENCY_SYMBOLS[currency.toUpperCase()];
  if (sym) return `${sym}${rounded}`;
  const parts = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    currencyDisplay: 'narrowSymbol',
    notation: 'compact',
    maximumFractionDigits: 1,
  }).formatToParts(n);
  return parts.map((part) => part.value).join('');
}

export function formatSignedCompactCurrencyCode(
  n: number | null | undefined,
  currency: string | null | undefined,
): string {
  if (n == null || !Number.isFinite(n)) return '—';
  const sign = n > 0 ? '+' : n < 0 ? '−' : '';
  return `${sign}${formatCompactCurrencyCode(Math.abs(n), currency)}`;
}

/** "just now" / "3m ago" / "2h ago" relative to now, for the last-updated indicator. */
export function formatRelativeTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '—';
  const seconds = Math.max(0, Math.round((Date.now() - then) / 1000));
  if (seconds < 10) return 'just now';
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.round(hours / 24)}d ago`;
}

export function formatNumber(n: number | null | undefined, digits = 2): string {
  if (n == null || !Number.isFinite(n)) return '—';
  return nf({ minimumFractionDigits: digits, maximumFractionDigits: digits }).format(n);
}

export function formatShares(n: number | null | undefined): string {
  if (n == null || !Number.isFinite(n)) return '—';
  // Trim trailing zeros, up to 6 decimal places
  return nf({ minimumFractionDigits: 0, maximumFractionDigits: 6 }).format(n);
}

export function formatCompactNumber(n: number | null | undefined): string {
  if (n == null || !Number.isFinite(n)) return '—';
  return compactFmt.format(n);
}

/** Human-readable label for FX conversion status. */
export function formatFxStatus(status: string | null | undefined): string {
  if (!status) return '';
  switch (status) {
    case 'stale':
      return 'Stale rate';
    case 'unavailable':
      return 'Rate unavailable';
    default:
      return '';
  }
}

/** Short label for currency source. */
export function formatCurrencySource(source: string | null | undefined): string {
  if (!source) return '';
  switch (source) {
    case 'instrument':
      return 'From instrument';
    case 'manual':
      return 'Manual';
    case 'import':
      return 'From import';
    case 'user_base_backfill':
      return 'Backfilled';
    default:
      return '';
  }
}

const dateFmt = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
});

export function formatDateISO(iso: string | null | undefined): string {
  if (!iso) return '—';
  // "2024-02-14" → "14 Feb 2024" — editorial date style
  const d = new Date(iso + 'T00:00:00Z');
  if (Number.isNaN(d.getTime())) return iso;
  return dateFmt.format(d).replace(/,/g, '');
}

export function todayISO(): string {
  return new Date().toISOString().slice(0, 10);
}
