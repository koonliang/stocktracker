import { describe, expect, it } from 'vitest';
import {
  formatCompactNumber,
  formatCurrency,
  formatCurrencyCode,
  formatDateISO,
  formatNumber,
  formatPercent,
  formatShares,
  formatSignedCurrency,
  formatSignedCurrencyCode,
  formatSignedPercent,
} from '@/lib/format';

describe('format', () => {
  it('formats currency with cents by default', () => {
    expect(formatCurrency(1234.5)).toBe('$1,234.50');
    expect(formatCurrency(0)).toBe('$0.00');
    expect(formatCurrency(-42.1)).toMatch(/-\$?42\.10|\(\$42\.10\)/);
  });

  it('formats currency without cents when requested', () => {
    expect(formatCurrency(1234.5, { cents: false })).toBe('$1,235');
  });

  it('returns em-dash for null/NaN', () => {
    expect(formatCurrency(null)).toBe('—');
    expect(formatCurrency(undefined)).toBe('—');
    expect(formatCurrency(Number.NaN)).toBe('—');
    expect(formatPercent(null)).toBe('—');
    expect(formatNumber(Number.NaN)).toBe('—');
    expect(formatShares(undefined)).toBe('—');
    expect(formatCompactNumber(null)).toBe('—');
    expect(formatDateISO(null)).toBe('—');
    expect(formatDateISO(undefined)).toBe('—');
  });

  it('formats signed currency with typographic minus', () => {
    expect(formatSignedCurrency(100)).toBe('+$100.00');
    expect(formatSignedCurrency(-100)).toBe('−$100.00');
    expect(formatSignedCurrency(0)).toBe('$0.00');
  });

  it('formats percent and signed percent', () => {
    expect(formatPercent(0.1234)).toBe('12.34%');
    expect(formatSignedPercent(0.05)).toBe('+5.00%');
    expect(formatSignedPercent(-0.025)).toBe('−2.50%');
  });

  it('formats compact numbers', () => {
    expect(formatCompactNumber(1_500_000)).toBe('1.5M');
    expect(formatCompactNumber(2_100_000_000)).toBe('2.1B');
  });

  it('formats shares without trailing zeros', () => {
    expect(formatShares(10)).toBe('10');
    expect(formatShares(10.5)).toBe('10.5');
    expect(formatShares(0.123456)).toBe('0.123456');
  });

  it('formats ISO dates to editorial style', () => {
    expect(formatDateISO('2024-02-14')).toBe('14 Feb 2024');
  });

  it('uses narrow currency symbol for non-USD currencies', () => {
    // Symbol-map currencies (dollar variants)
    expect(formatCurrencyCode(1234.5, 'SGD')).toBe('S$1,234.50');
    expect(formatCurrencyCode(1234.5, 'AUD')).toBe('A$1,234.50');
    expect(formatCurrencyCode(1234.5, 'HKD')).toBe('HK$1,234.50');
    expect(formatCurrencyCode(1234.5, 'USD')).toBe('$1,234.50');
    // Intl narrowSymbol currencies
    expect(formatCurrencyCode(1234.5, 'EUR')).toBe('€1,234.50');
    expect(formatCurrencyCode(1234.5, 'GBP')).toBe('£1,234.50');
    // Signed variants
    expect(formatSignedCurrencyCode(1234.5, 'SGD')).toBe('+S$1,234.50');
    expect(formatSignedCurrencyCode(-500, 'EUR')).toBe('−€500.00');
  });

  it('handles very large and zero values', () => {
    expect(formatNumber(0)).toBe('0.00');
    expect(formatNumber(1234567.89, 0)).toBe('1,234,568');
  });
});
