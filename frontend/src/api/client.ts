const API_BASE = '/api';

export class ApiError extends Error {
  readonly status: number;
  readonly body: unknown;

  constructor(message: string, status: number, body: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

async function readErrorBody(response: Response): Promise<unknown> {
  const contentType = response.headers.get('content-type') ?? '';

  if (contentType.includes('application/json')) {
    try {
      return await response.json();
    } catch {
      return null;
    }
  }

  try {
    const text = await response.text();
    return text || null;
  } catch {
    return null;
  }
}

export function getErrorMessage(error: unknown, fallbackMessage: string): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallbackMessage;
}

/**
 * Type-safe HTTP client for the backend API.
 *
 * Note: Returns `undefined` cast to `T` for 204 (No Content) responses.
 * Callers expecting 204 should check for undefined explicitly.
 */
export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T | undefined> {
  const token = localStorage.getItem('jwt');

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const body = await readErrorBody(response);
    const message = isRecord(body) && typeof body.error === 'string'
      ? body.error
      : typeof body === 'string' && body.trim()
        ? body
        : `API error: ${response.status}`;
    throw new ApiError(message, response.status, body);
  }

  if (response.status === 204) {
    return undefined;
  }

  return response.json();
}
