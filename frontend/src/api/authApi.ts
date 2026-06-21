import { apiRequest } from './client';
import type {
  AuthUser,
  DemoUserCatalog,
  DemoUserSession,
  LoginResponse,
  SocialExchangeRequest,
  StatusResponse,
} from './types';

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

export function forgotPassword(email: string) {
  return apiRequest<StatusResponse>('/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

export function resetPassword(token: string, newPassword: string) {
  return apiRequest<StatusResponse>('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword }),
  });
}

export function fetchMe() {
  return apiRequest<AuthUser>('/auth/me');
}

export function logout() {
  return apiRequest<void>('/auth/logout', { method: 'POST' });
}

export function exchangeSocialCode(
  provider: 'google' | 'facebook',
  payload: SocialExchangeRequest,
) {
  return apiRequest<LoginResponse>(`/auth/social/${provider}/exchange`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function listDemoUsers() {
  return apiRequest<DemoUserCatalog>('/auth/demo-users');
}

export function createDemoUser(label?: string) {
  return apiRequest<DemoUserSession>('/auth/demo-users', {
    method: 'POST',
    body: JSON.stringify(label ? { label } : {}),
  });
}

export function loginDemoUser(slot: number) {
  return apiRequest<DemoUserSession>(`/auth/demo-users/${slot}/login`, {
    method: 'POST',
  });
}
