import { apiRequest } from './client';
import type { InstrumentAnalysisResponse } from './types';
import type { TimeRange } from '@/lib/types';

export function getInstrumentAnalysis(ticker: string, range: TimeRange = '1Y') {
  return apiRequest<InstrumentAnalysisResponse>(
    `/instruments/${encodeURIComponent(ticker)}?range=${encodeURIComponent(range)}`,
  );
}
