import { apiRequest } from './client';
import type { AuthUser, LoginResponse } from './types';

export function login(email: string, password: string) {
  return apiRequest<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export function fetchMe() {
  return apiRequest<AuthUser>('/auth/me');
}

export function logout() {
  return apiRequest<void>('/auth/logout', { method: 'POST' });
}
