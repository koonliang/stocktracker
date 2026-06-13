import { apiRequest } from './client';
import type {
  DashboardResponse,
  Transaction,
  TransactionImportNormalizedRow,
  TransactionImportPreviewResponse,
} from './types';

export function getTransactions() {
  return apiRequest<Transaction[]>('/transactions');
}

export function deleteTransaction(id: string) {
  return apiRequest<DashboardResponse>(`/transactions/${id}`, { method: 'DELETE' });
}

export function createTransaction(row: TransactionImportNormalizedRow) {
  return apiRequest<DashboardResponse>('/transactions', {
    method: 'POST',
    body: JSON.stringify({ rows: [row] }),
  });
}

export async function previewTransactionImport(file: File) {
  const form = new FormData();
  form.append('file', file);
  return apiRequest<TransactionImportPreviewResponse>('/transactions/import/preview', {
    method: 'POST',
    body: form,
  });
}

export function commitTransactionImport(rows: TransactionImportNormalizedRow[]) {
  return apiRequest<DashboardResponse>('/transactions/import/commit', {
    method: 'POST',
    body: JSON.stringify({ rows }),
  });
}

export function exportTransactionsCsv() {
  // Goes through apiRequest so the Authorization header is attached (the endpoint
  // is now behind auth); the CSV body is returned as text since it isn't JSON.
  return apiRequest<string>('/transactions/export');
}
