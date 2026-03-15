import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { apiFetch, ApiError, getErrorMessage } from '../api/client';

describe('apiFetch', () => {
  let mockFetch: ReturnType<typeof vi.fn>;
  let storage: Record<string, string>;

  beforeEach(() => {
    storage = {};
    mockFetch = vi.fn();
    vi.stubGlobal('fetch', mockFetch);
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => storage[key] ?? null,
      setItem: (key: string, value: string) => { storage[key] = value; },
      removeItem: (key: string) => { delete storage[key]; },
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('includes Authorization header when JWT exists', async () => {
    storage['jwt'] = 'test-token';
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ id: 1 }),
    });

    await apiFetch('/user/me');

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/user/me',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer test-token',
        }),
      }),
    );
  });

  it('omits Authorization header when no JWT', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ id: 1 }),
    });

    await apiFetch('/user/me');

    const headers = mockFetch.mock.calls[0][1].headers;
    expect(headers).not.toHaveProperty('Authorization');
  });

  it('throws ApiError with JSON error body', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 400,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () => Promise.resolve({ error: 'Bad request' }),
    });

    await expect(apiFetch('/auth/verify')).rejects.toThrow(ApiError);

    mockFetch.mockResolvedValue({
      ok: false,
      status: 400,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () => Promise.resolve({ error: 'Bad request' }),
    });

    await expect(apiFetch('/auth/verify')).rejects.toMatchObject({
      status: 400,
      message: 'Bad request',
    });
  });

  it('throws ApiError with text error body', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 500,
      headers: new Headers({ 'content-type': 'text/plain' }),
      text: () => Promise.resolve('Internal Server Error'),
    });

    await expect(apiFetch('/user/me')).rejects.toMatchObject({
      status: 500,
      message: 'Internal Server Error',
    });
  });

  it('returns parsed JSON on success', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ email: 'test@example.com' }),
    });

    const result = await apiFetch<{ email: string }>('/user/me');
    expect(result).toEqual({ email: 'test@example.com' });
  });

  it('returns undefined for 204 No Content', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 204,
    });

    const result = await apiFetch('/some/endpoint');
    expect(result).toBeUndefined();
  });
});

describe('getErrorMessage', () => {
  it('extracts message from ApiError', () => {
    const err = new ApiError('Something broke', 400, null);
    expect(getErrorMessage(err, 'fallback')).toBe('Something broke');
  });

  it('extracts message from Error', () => {
    expect(getErrorMessage(new Error('oops'), 'fallback')).toBe('oops');
  });

  it('returns fallback for unknown errors', () => {
    expect(getErrorMessage('string error', 'fallback')).toBe('fallback');
    expect(getErrorMessage(null, 'fallback')).toBe('fallback');
    expect(getErrorMessage(undefined, 'fallback')).toBe('fallback');
    expect(getErrorMessage(42, 'fallback')).toBe('fallback');
  });
});

describe('ApiError', () => {
  it('has correct name, status, and body', () => {
    const error = new ApiError('Not Found', 404, { detail: 'missing' });
    expect(error.name).toBe('ApiError');
    expect(error.status).toBe(404);
    expect(error.body).toEqual({ detail: 'missing' });
    expect(error.message).toBe('Not Found');
    expect(error).toBeInstanceOf(Error);
  });
});
