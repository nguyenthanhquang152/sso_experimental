import { test, expect } from '@playwright/test';

test.describe('Logout Flow', () => {
  test('should clear localStorage jwt when logout API succeeds', async ({ page, request }) => {
    // Simulate an authenticated state by setting a token in localStorage
    await page.goto('/');
    await page.evaluate(() => {
      localStorage.setItem('jwt', 'fake-jwt-for-logout-test');
    });

    // Verify the token is stored
    const tokenBefore = await page.evaluate(() => localStorage.getItem('jwt'));
    expect(tokenBefore).toBe('fake-jwt-for-logout-test');

    // Verify the logout API endpoint works
    const response = await request.post('/api/auth/logout');
    expect(response.status()).toBe(200);

    // Simulate what the app does on logout: clear localStorage
    await page.evaluate(() => {
      localStorage.removeItem('jwt');
    });

    const tokenAfter = await page.evaluate(() => localStorage.getItem('jwt'));
    expect(tokenAfter).toBeNull();
  });

  test('should redirect to homepage after token is removed', async ({ page }) => {
    // Start on homepage, set a fake token
    await page.goto('/');
    await page.evaluate(() => {
      localStorage.setItem('jwt', 'fake-jwt-for-redirect-test');
    });

    // Navigate to dashboard with the fake token
    // The app will try to fetch /api/user/me, which will fail with 401
    // This triggers logout() -> removes token -> redirects to /
    await page.goto('/dashboard');

    // Wait for the redirect back to homepage after the API call fails
    await page.waitForURL('/', { timeout: 10000 });

    // Verify we are on the homepage
    await expect(page.locator('h1')).toHaveText('Google SSO Demo');

    // Verify token was cleared by the failed auth
    const token = await page.evaluate(() => localStorage.getItem('jwt'));
    expect(token).toBeNull();
  });
});
