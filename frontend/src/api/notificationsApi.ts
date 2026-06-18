import { apiRequest } from './client';
import type { NotificationListResponse, ReadAllResponse } from './types';

export function listNotifications(unread = false): Promise<NotificationListResponse> {
  return apiRequest<NotificationListResponse>(`/notifications${unread ? '?unread=true' : ''}`);
}

export function listNotificationsWithLimit(limit: number): Promise<NotificationListResponse> {
  return apiRequest<NotificationListResponse>(`/notifications?limit=${limit}`);
}

export function markNotificationRead(id: string): Promise<void> {
  return apiRequest<void>(`/notifications/${id}/read`, { method: 'POST' });
}

export function markAllNotificationsRead(ids?: string[]): Promise<ReadAllResponse> {
  return apiRequest<ReadAllResponse>('/notifications/read-all', {
    method: 'POST',
    body: ids && ids.length > 0 ? JSON.stringify({ ids }) : JSON.stringify({}),
  });
}

export function deleteNotification(id: string): Promise<void> {
  return apiRequest<void>(`/notifications/${id}`, { method: 'DELETE' });
}
