import { apiRequest } from './client';
import type { AuthUser, LoginResponse, StatusResponse } from './types';

export function login(email: string, password: string) {
  return apiRequest<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export function signup(email: string, password: string) {
  return apiRequest<StatusResponse>('/auth/signup', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export function verifyEmail(token: string) {
  return apiRequest<StatusResponse>('/auth/verify-email', {
    method: 'POST',
    body: JSON.stringify({ token }),
  });
}

export function resendVerification(email: string) {
  return apiRequest<StatusResponse>('/auth/resend-verification', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

export function fetchMe() {
  return apiRequest<AuthUser>('/auth/me');
}

export function logout() {
  return apiRequest<void>('/auth/logout', { method: 'POST' });
}
