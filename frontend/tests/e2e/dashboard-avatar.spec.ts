import { test, expect } from '@playwright/test';

test.describe('Dashboard Avatar', () => {
  test('should render a fallback avatar when pictureUrl is missing', async ({ page }) => {
    await page.route('**/api/user/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 1,
          email: 'microsoft.user@example.com',
          name: 'Microsoft User',
          pictureUrl: '',
          lastLoginFlow: 'CLIENT_SIDE',
          createdAt: '2026-03-13T10:00:00Z',
          lastLoginAt: '2026-03-13T10:05:00Z',
        }),
      });
    });

    await page.addInitScript(() => {
      localStorage.setItem('jwt', 'fake-microsoft-jwt');
    });
    await page.goto('/dashboard');

    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByRole('img', { name: 'Avatar fallback for Microsoft User' })).toBeVisible();
    await expect(page.getByText('MU')).toBeVisible();
  });
});
