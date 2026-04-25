import { apiRequest } from './client';
import type { InstrumentAnalysisResponse } from './types';

export function getInstrumentAnalysis(ticker: string) {
  return apiRequest<InstrumentAnalysisResponse>(`/instruments/${encodeURIComponent(ticker)}`);
}
