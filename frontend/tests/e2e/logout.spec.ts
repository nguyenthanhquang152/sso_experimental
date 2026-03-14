import { test, expect } from '@playwright/test';

test.describe('Logout Flow', () => {
  test('should clear localStorage jwt when the logout button is clicked', async ({ page }) => {
    await page.route('**/api/user/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 1,
          email: 'logout@example.com',
          name: 'Logout User',
          pictureUrl: null,
          lastLoginFlow: 'CLIENT_SIDE',
          createdAt: '2026-03-13T10:00:00Z',
          lastLoginAt: '2026-03-13T10:05:00Z',
        }),
      });
    });

    // Simulate an authenticated state before the app boots
    await page.addInitScript(() => {
      localStorage.setItem('jwt', 'fake-jwt-for-logout-test');
    });

    await page.goto('/dashboard');
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();

    const tokenBefore = await page.evaluate(() => localStorage.getItem('jwt'));
    expect(tokenBefore).toBe('fake-jwt-for-logout-test');

    await page.getByRole('button', { name: 'Logout' }).click();
    await page.waitForURL('/');

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
    await expect(page.locator('h1')).toHaveText('SSO Demo');

    // Verify token was cleared by the failed auth
    const token = await page.evaluate(() => localStorage.getItem('jwt'));
    expect(token).toBeNull();
  });
});
