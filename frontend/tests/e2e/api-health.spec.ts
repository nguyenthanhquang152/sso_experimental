import { test, expect } from '@playwright/test';

const apiBaseURL = process.env.PLAYWRIGHT_API_BASE_URL;

test.describe('API Health Checks', () => {
  test.beforeEach(() => {
    test.skip(!apiBaseURL, 'Set PLAYWRIGHT_API_BASE_URL to run backend API health checks.');
  });

  test('POST /api/auth/logout should return 200', async ({ playwright }) => {
    const request = await playwright.request.newContext({ baseURL: apiBaseURL });
    const response = await request.post('/api/auth/logout');
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body).toHaveProperty('message');
    expect(body.message).toContain('Logged out');
    await request.dispose();
  });

  test('GET /api/user/me without token should return 401', async ({ playwright }) => {
    const request = await playwright.request.newContext({ baseURL: apiBaseURL });
    const response = await request.get('/api/user/me');
    expect(response.status()).toBe(401);
    await request.dispose();
  });

  test('GET /api/user/me with invalid token should return 401', async ({ playwright }) => {
    const request = await playwright.request.newContext({ baseURL: apiBaseURL });
    const response = await request.get('/api/user/me', {
      headers: {
        Authorization: 'Bearer invalid-token-abc123',
      },
    });
    expect(response.status()).toBe(401);
    await request.dispose();
  });

  test('POST /api/auth/google/verify without credential should return 4xx', async ({
    playwright,
  }) => {
    const request = await playwright.request.newContext({ baseURL: apiBaseURL });
    const response = await request.post('/api/auth/google/verify', {
      data: {},
      headers: { 'Content-Type': 'application/json' },
    });
    // Should reject with 400 (bad request) or 401 (unauthorized)
    expect(response.status()).toBeGreaterThanOrEqual(400);
    expect(response.status()).toBeLessThan(500);
    await request.dispose();
  });
});
