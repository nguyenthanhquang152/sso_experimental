import { test, expect } from '@playwright/test';

test.describe('Protected Route - Dashboard', () => {
  test('should redirect unauthenticated user from /dashboard to /', async ({ page }) => {
    // Clear any existing auth state
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());

    // Navigate to the protected dashboard route
    await page.goto('/dashboard');

    // The DashboardPage component checks isAuthenticated and redirects to /
    // Wait for navigation to complete
    await page.waitForURL('/');

    // Verify we are on the homepage with login cards
    await expect(page.locator('h1')).toHaveText('Google SSO Demo');
  });

  test('should not show dashboard content when unauthenticated', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/dashboard');

    // Wait for redirect
    await page.waitForURL('/');

    // Dashboard heading should not be present
    await expect(page.getByRole('heading', { name: 'Dashboard' })).not.toBeVisible();
  });
});
