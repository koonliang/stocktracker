import { apiRequest } from './client';
import type { AlertListResponse, AlertRequest, Alert } from './types';

export function listAlerts(): Promise<AlertListResponse> {
  return apiRequest<AlertListResponse>('/alerts');
}

export function createAlert(request: AlertRequest): Promise<Alert> {
  return apiRequest<Alert>('/alerts', { method: 'POST', body: JSON.stringify(request) });
}

export function updateAlert(id: string, request: AlertRequest): Promise<Alert> {
  return apiRequest<Alert>(`/alerts/${id}`, { method: 'PATCH', body: JSON.stringify(request) });
}

export function deleteAlert(id: string): Promise<void> {
  return apiRequest<void>(`/alerts/${id}`, { method: 'DELETE' });
}
