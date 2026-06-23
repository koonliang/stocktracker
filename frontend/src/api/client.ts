export type ApiErrorPayload = {
  code: string;
  message: string;
  details?: Record<string, unknown> | null;
};

export class ApiError extends Error {
  status: number;
  code: string;
  details?: Record<string, unknown> | null;

  constructor(status: number, payload: ApiErrorPayload) {
    super(payload.message);
    this.name = 'ApiError';
    this.status = status;
    this.code = payload.code;
    this.details = payload.details;
  }
}

export function getApiErrorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return 'Request failed';
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';
const RETRYABLE_METHODS = new Set(['GET', 'HEAD']);
const RETRYABLE_STATUSES = new Set([502, 503, 504]);

// Decoupled session hooks (set by authStore) so the client never imports the
// store — avoids an import cycle and keeps the fetch wrapper framework-agnostic.
let authToken: string | null = null;
let unauthorizedHandler: (() => void) | null = null;

export function setAuthToken(token: string | null): void {
  authToken = token;
}

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler;
}

function buildUrl(path: string): string {
  return `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
}

async function parseBody<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }
  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    return (await response.json()) as T;
  }
  return (await response.text()) as T;
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const method = (init?.method ?? 'GET').toUpperCase();
  const attempts = RETRYABLE_METHODS.has(method) ? 2 : 1;
  let response: Response | undefined;

  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      response = await fetch(buildUrl(path), {
        ...init,
        headers: {
          ...(init?.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
          ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
          ...(init?.headers ?? {}),
        },
      });
    } catch (error) {
      if (attempt < attempts) {
        continue;
      }
      throw error;
    }

    if (response.ok || !RETRYABLE_STATUSES.has(response.status) || attempt === attempts) {
      break;
    }
  }

  if (!response) {
    throw new Error('Request failed without a response');
  }

  if (!response.ok) {
    // A 401 on a non-auth endpoint means the session is gone — clear it and let
    // ProtectedRoute redirect. Auth endpoints (e.g. wrong password) handle their
    // own 401 inline, so they are exempt.
    if (response.status === 401 && !path.startsWith('/auth') && unauthorizedHandler) {
      unauthorizedHandler();
    }
    const payload = await parseBody<ApiErrorPayload>(response).catch(() => ({
      code: 'request_failed',
      message: 'Request failed',
    }));
    throw new ApiError(response.status, payload);
  }

  return parseBody<T>(response);
}

export function apiUrl(path: string): string {
  return buildUrl(path);
}
