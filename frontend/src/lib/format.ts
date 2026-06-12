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

export function formatCurrencyCode(
  n: number | null | undefined,
  currency: string | null | undefined,
  opts?: { cents?: boolean },
): string {
  if (n == null || !Number.isFinite(n)) return '—';
  if (!currency) return formatCurrency(n, opts);
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    maximumFractionDigits: opts?.cents === false ? 0 : 2,
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
