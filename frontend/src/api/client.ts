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

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';
const RETRYABLE_METHODS = new Set(['GET', 'HEAD']);
const RETRYABLE_STATUSES = new Set([502, 503, 504]);

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
