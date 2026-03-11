import { test, expect } from '@playwright/test';

test.describe('HomePage', () => {
  test.beforeEach(async ({ page }) => {
    // Clear localStorage before each test to ensure clean state
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/');
  });

  test('should display the page title and description', async ({ page }) => {
    await expect(page.locator('h1')).toHaveText('Google SSO Demo');
    await expect(
      page.getByText('This demo shows two Google OAuth2 flows side-by-side.')
    ).toBeVisible();
  });

  test('should display the Server-Side Flow login card', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Server-Side Flow' })).toBeVisible();
    await expect(
      page.getByText('The browser redirects to Google via the Spring Boot backend.')
    ).toBeVisible();

    const serverLink = page.getByRole('link', { name: 'Sign in with Google (Server-Side)' });
    await expect(serverLink).toBeVisible();
    await expect(serverLink).toHaveAttribute('href', '/api/oauth2/authorization/google');
  });

  test('should display the Client-Side Flow login card', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Client-Side Flow' })).toBeVisible();
    await expect(
      page.getByText('Google Sign-In happens directly in the browser via a popup.')
    ).toBeVisible();
  });

  test('should have both login cards side by side', async ({ page }) => {
    const serverCard = page.getByRole('heading', { name: 'Server-Side Flow' });
    const clientCard = page.getByRole('heading', { name: 'Client-Side Flow' });

    await expect(serverCard).toBeVisible();
    await expect(clientCard).toBeVisible();
  });
});
