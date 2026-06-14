import { apiRequest } from './client';
import type { PerformanceResponse } from './types';

export type PerformanceWindow = '1M' | '3M' | '6M' | '1Y' | 'YTD' | 'ALL';
export type LotMethod = 'fifo' | 'lifo';

export function getPerformance(
  window: PerformanceWindow,
  method: LotMethod,
): Promise<PerformanceResponse> {
  return apiRequest<PerformanceResponse>(`/performance?window=${window}&method=${method}`);
}
