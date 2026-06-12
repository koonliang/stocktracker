import { apiRequest } from './client';
import type { BaseCurrencyResponse } from './types';

export function getBaseCurrency() {
  return apiRequest<BaseCurrencyResponse>('/me/base-currency');
}

export function updateBaseCurrency(baseCurrency: string) {
  return apiRequest<BaseCurrencyResponse>('/me/base-currency', {
    method: 'PUT',
    body: JSON.stringify({ baseCurrency }),
  });
}
