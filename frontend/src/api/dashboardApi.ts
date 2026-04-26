import { apiRequest } from './client';
import type { DashboardResponse } from './types';

export function getDashboard() {
  return apiRequest<DashboardResponse>('/dashboard');
}
