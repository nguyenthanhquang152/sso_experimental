import { test, expect } from '@playwright/test';

test.describe('HomePage', () => {
  const googleOnlyProviders = {
    google: {
      serverSideEnabled: true,
      clientSideEnabled: true,
      clientId: 'google-client-id.apps.googleusercontent.com',
    },
    microsoft: {
      serverSideEnabled: false,
      clientSideEnabled: false,
      scopes: [],
    },
  };

  test.beforeEach(async ({ page }) => {
    // Clear localStorage before each test to ensure clean state
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/');
  });

  test('should render the provider-agnostic title and fall back to server-side Google when provider config fails', async ({ page }) => {
    await page.route('**/api/auth/providers', async (route) => {
      await route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'temporarily unavailable' }),
      });
    });

    await page.goto('/');

    await expect(page.locator('h1')).toHaveText('SSO Demo');
    await expect(
      page.getByText('Choose a provider and flow to sign in.')
    ).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Google · Server-Side Flow' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Google · Client-Side Flow' })).not.toBeVisible();
    await expect(page.getByRole('heading', { name: 'Microsoft · Server-Side Flow' })).not.toBeVisible();
    await expect(page.getByRole('heading', { name: 'Microsoft · Client-Side Flow' })).not.toBeVisible();
  });

  test('should display only Google cards when Microsoft is disabled by runtime config', async ({ page }) => {
    await page.route('**/api/auth/providers', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(googleOnlyProviders),
      });
    });

    await page.goto('/');

    const serverLink = page.getByRole('link', { name: 'Continue with Google (Server-Side)' });
    await expect(serverLink).toBeVisible();
    await expect(serverLink).toHaveAttribute('href', '/api/oauth2/authorization/google');
    await expect(page.getByRole('heading', { name: 'Google · Client-Side Flow' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Microsoft · Server-Side Flow' })).not.toBeVisible();
    await expect(page.getByRole('heading', { name: 'Microsoft · Client-Side Flow' })).not.toBeVisible();
  });

  test('should display four cards when Microsoft flows are enabled by runtime config', async ({ page }) => {
    await page.route('**/api/auth/providers', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          google: {
            serverSideEnabled: true,
            clientSideEnabled: true,
            clientId: 'google-client-id.apps.googleusercontent.com',
          },
          microsoft: {
            serverSideEnabled: true,
            clientSideEnabled: true,
            clientId: 'microsoft-client-id',
            authority: 'https://login.microsoftonline.com/common/v2.0',
            scopes: ['openid', 'profile', 'email'],
          },
        }),
      });
    });

    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'Google · Server-Side Flow' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Google · Client-Side Flow' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Microsoft · Server-Side Flow' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Microsoft · Client-Side Flow' })).toBeVisible();

    await expect(
      page.getByRole('link', { name: 'Continue with Microsoft (Server-Side)' })
    ).toHaveAttribute('href', '/api/oauth2/authorization/microsoft');
  });

  test('should send the configured app root as redirect_uri for Microsoft client-side login', async ({ page }) => {
    await page.route('**/api/auth/providers', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          google: {
            serverSideEnabled: true,
            clientSideEnabled: true,
            clientId: 'google-client-id.apps.googleusercontent.com',
          },
          microsoft: {
            serverSideEnabled: true,
            clientSideEnabled: true,
            clientId: 'microsoft-client-id',
            authority: 'https://login.microsoftonline.com/common/v2.0',
            scopes: ['openid', 'profile', 'email'],
          },
        }),
      });
    });

    await page.route('**/api/auth/microsoft/challenge', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          challengeId: 'test-challenge-id',
          nonce: 'test-nonce',
        }),
      });
    });

    await page.goto('/');

    const popupPromise = page.waitForEvent('popup');
    await page.getByRole('button', { name: 'Continue with Microsoft' }).click();
    const popup = await popupPromise;
    await popup.waitForURL(/login\.microsoftonline\.com\/.*\/oauth2\/v2\.0\/authorize/);

    const authorizeUrl = new URL(popup.url());
    expect(authorizeUrl.searchParams.get('redirect_uri')).toBe('http://localhost:8000');
    expect(authorizeUrl.searchParams.get('nonce')).toBe('test-nonce');

    await popup.close();
    await expect(page.getByRole('alert')).toHaveText('Microsoft sign-in failed. Please try again.');
  });
});
