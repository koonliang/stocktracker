import Papa from 'papaparse';
import { z } from 'zod';
import type { Transaction } from './types';
import { isKnownTicker } from './seed';

const CANONICAL_HEADER = ['date', 'ticker', 'type', 'quantity', 'price', 'fees'] as const;

export type ParsedRow = {
  row: number; // 1-based, header is row 1; first data row is 2
  raw: Record<string, string>;
};

export type InvalidRow = ParsedRow & { reason: string };

export type ParseResult = {
  valid: Transaction[];
  invalid: InvalidRow[];
  /** Header issues that prevented per-row validation, e.g. missing required column. */
  headerErrors: string[];
};

const isoDateRegex = /^\d{4}-\d{2}-\d{2}$/;

export const transactionRowSchema = z
  .object({
    date: z
      .string()
      .trim()
      .refine((v) => isoDateRegex.test(v), 'date must be YYYY-MM-DD')
      .refine((v) => !Number.isNaN(new Date(`${v}T00:00:00Z`).getTime()), 'date is not a real date')
      .refine((v) => new Date(`${v}T00:00:00Z`).getTime() <= Date.now(), 'date is in the future'),
    ticker: z
      .string()
      .trim()
      .transform((v) => v.toUpperCase())
      .refine((v) => /^[A-Z]{1,5}$/.test(v), 'ticker must be 1–5 uppercase letters')
      .refine(
        (v) => isKnownTicker(v),
        (v) => ({ message: `unknown ticker: ${v}` }),
      ),
    type: z
      .string()
      .trim()
      .transform((v) => v.toLowerCase())
      .refine((v) => v === 'buy' || v === 'sell', 'type must be buy or sell'),
    quantity: z
      .string()
      .trim()
      .refine((v) => v.length > 0, 'quantity is required')
      .refine((v) => !Number.isNaN(Number(v)), 'malformed number in quantity')
      .transform((v) => Number(v))
      .refine((n) => n > 0, 'quantity must be > 0'),
    price: z
      .string()
      .trim()
      .refine((v) => v.length > 0, 'price is required')
      .refine((v) => !Number.isNaN(Number(v)), 'malformed number in price')
      .transform((v) => Number(v))
      .refine((n) => n > 0, 'price must be > 0'),
    fees: z
      .string()
      .trim()
      .optional()
      .transform((v) => (v == null || v === '' ? 0 : Number(v)))
      .refine((n) => !Number.isNaN(n), 'malformed number in fees')
      .refine((n) => n >= 0, 'fees must be >= 0'),
  })
  .transform((row) => ({
    date: row.date,
    ticker: row.ticker as string,
    type: row.type as 'buy' | 'sell',
    quantity: row.quantity,
    price: row.price,
    fees: row.fees,
  }));

function newId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID();
  return `tx_${Math.random().toString(36).slice(2, 10)}_${Date.now().toString(36)}`;
}

export function parseTransactionsCSV(text: string): ParseResult {
  // Strip optional UTF-8 BOM (U+FEFF) if present.
  const cleaned = text.charCodeAt(0) === 0xfeff ? text.slice(1) : text;
  const result = Papa.parse<Record<string, string>>(cleaned, {
    header: true,
    skipEmptyLines: 'greedy',
    transformHeader: (h) => h.trim().toLowerCase(),
  });

  const valid: Transaction[] = [];
  const invalid: InvalidRow[] = [];
  const headerErrors: string[] = [];

  const fields = (result.meta.fields ?? []).map((f) => f.toLowerCase());
  for (const required of ['date', 'ticker', 'type', 'quantity', 'price'] as const) {
    if (!fields.includes(required)) {
      headerErrors.push(`missing required column: ${required}`);
    }
  }
  if (headerErrors.length > 0) {
    return { valid, invalid, headerErrors };
  }

  const rows = result.data;
  rows.forEach((raw, idx) => {
    const rowNumber = idx + 2; // header is row 1
    const parsed = transactionRowSchema.safeParse({
      date: raw.date ?? '',
      ticker: raw.ticker ?? '',
      type: raw.type ?? '',
      quantity: raw.quantity ?? '',
      price: raw.price ?? '',
      fees: raw.fees,
    });
    if (parsed.success) {
      valid.push({ id: newId(), ...parsed.data });
    } else {
      const reason = parsed.error.issues[0]?.message ?? 'invalid row';
      invalid.push({ row: rowNumber, raw, reason });
    }
  });

  return { valid, invalid, headerErrors };
}

function csvEscape(value: string): string {
  if (/[",\n\r]/.test(value)) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
}

function formatNumberCell(n: number, maxDecimals: number): string {
  // Trim trailing zeros but keep up to maxDecimals
  const s = n.toFixed(maxDecimals);
  return s.replace(/\.?0+$/, '') || '0';
}

export function serializeTransactionsCSV(transactions: Transaction[]): string {
  const lines = [CANONICAL_HEADER.join(',')];
  for (const t of transactions) {
    lines.push(
      [
        csvEscape(t.date),
        csvEscape(t.ticker.toUpperCase()),
        csvEscape(t.type.toLowerCase()),
        formatNumberCell(t.quantity, 6),
        formatNumberCell(t.price, 4),
        formatNumberCell(t.fees, 4),
      ].join(','),
    );
  }
  return lines.join('\n') + '\n';
}
