import { apiRequest, apiUrl } from './client';
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

export async function exportTransactionsCsv() {
  const response = await fetch(apiUrl('/transactions/export'));
  if (!response.ok) {
    throw new Error('Export failed');
  }
  return response.text();
}
