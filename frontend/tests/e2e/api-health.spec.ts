import { test, expect } from '@playwright/test';

test.describe('API Health Checks', () => {
  test('POST /api/auth/logout should return 200', async ({ request }) => {
    const response = await request.post('/api/auth/logout');
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body).toHaveProperty('message');
    expect(body.message).toContain('Logged out');
  });

  test('GET /api/user/me without token should return 401', async ({ request }) => {
    const response = await request.get('/api/user/me');
    expect(response.status()).toBe(401);
  });

  test('GET /api/user/me with invalid token should return 401', async ({ request }) => {
    const response = await request.get('/api/user/me', {
      headers: {
        Authorization: 'Bearer invalid-token-abc123',
      },
    });
    expect(response.status()).toBe(401);
  });

  test('POST /api/auth/google/verify without credential should return 4xx', async ({
    request,
  }) => {
    const response = await request.post('/api/auth/google/verify', {
      data: {},
      headers: { 'Content-Type': 'application/json' },
    });
    // Should reject with 400 (bad request) or 401 (unauthorized)
    expect(response.status()).toBeGreaterThanOrEqual(400);
    expect(response.status()).toBeLessThan(500);
  });
});
