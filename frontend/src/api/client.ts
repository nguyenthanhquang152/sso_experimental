const API_BASE = '/api';

export class ApiError extends Error {
  public readonly status: number;
  public readonly statusText: string;
  public readonly body: unknown;

  constructor(status: number, statusText: string, body?: unknown) {
    super(`API error: ${status} ${statusText}`);
    this.name = 'ApiError';
    this.status = status;
    this.statusText = statusText;
    this.body = body;
  }
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
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
    const body = await response.json().catch(() => undefined);
    throw new ApiError(response.status, response.statusText, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}
