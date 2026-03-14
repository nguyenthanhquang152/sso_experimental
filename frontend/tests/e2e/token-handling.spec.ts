import { test, expect } from '@playwright/test';

test.describe('Auth Code Exchange Handling', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
  });

  test('should exchange code for token and store in localStorage', async ({ page }) => {
    // Mock the /api/auth/exchange endpoint to return a fake JWT
    await page.route('**/api/auth/exchange', async (route) => {
      const request = route.request();
      const body = JSON.parse(request.postData() || '{}');
      if (body.code === 'valid-test-code') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ token: 'test-jwt-token-12345' }),
        });
      } else {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Invalid or expired code' }),
        });
      }
    });

    // Mock /api/user/me to return user data so dashboard doesn't redirect back
    await page.route('**/api/user/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          email: 'test@example.com',
          name: 'Test User',
          lastLoginFlow: 'SERVER_SIDE',
          pictureUrl: null,
        }),
      });
    });

    // Navigate with ?code= parameter (simulating server-side OAuth redirect)
    await page.goto('/?code=valid-test-code');

    // Should exchange code for JWT and redirect to dashboard
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    const storedToken = await page.evaluate(() => localStorage.getItem('jwt'));
    expect(storedToken).toBe('test-jwt-token-12345');
  });

  test('should strip code from URL immediately', async ({ page }) => {
    // Mock exchange to simulate async delay
    await page.route('**/api/auth/exchange', async (route) => {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Invalid or expired code' }),
      });
    });

    await page.goto('/?code=some-code');

    // URL should be stripped of code parameter quickly
    await page.waitForURL('/', { timeout: 5000 });

    // Code should not be in the URL
    const url = page.url();
    expect(url).not.toContain('code=');
  });

  test('should not store anything when no code in URL', async ({ page }) => {
    await page.goto('/');

    const storedToken = await page.evaluate(() => localStorage.getItem('jwt'));
    expect(storedToken).toBeNull();
  });
});
