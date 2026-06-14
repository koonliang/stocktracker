import { apiRequest } from './client';
import type { NotificationListResponse } from './types';

export function listNotifications(unread = false): Promise<NotificationListResponse> {
  return apiRequest<NotificationListResponse>(`/notifications${unread ? '?unread=true' : ''}`);
}

export function markNotificationRead(id: string): Promise<void> {
  return apiRequest<void>(`/notifications/${id}/read`, { method: 'POST' });
}
