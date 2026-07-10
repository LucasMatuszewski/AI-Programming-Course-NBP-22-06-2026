import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright E2E configuration for Hardware Service Decision Copilot.
 *
 * Tests run against the real stack:
 *   - Frontend: http://localhost:4200  (Angular dev server, proxies /api → :8080)
 *   - Backend:  http://localhost:8080  (Spring Boot)
 *
 * Start both before running:
 *   Backend:  cd app/backend && mvnw.cmd spring-boot:run
 *   Frontend: cd app/frontend && npm run start
 */
export default defineConfig({
  testDir: './tests',
  testMatch: '**/*.spec.ts',

  // Run tests sequentially to avoid shared-state issues (in-memory session store)
  fullyParallel: false,
  workers: 1,

  // Fail the build if a test is focused (--only) on CI
  forbidOnly: !!process.env.CI,

  // Retry on CI only
  retries: process.env.CI ? 2 : 0,

  // Reporters
  reporter: [['html', { open: 'never' }], ['list']],

  use: {
    // Both frontend and backend must be running before tests start.
    // reuseExistingServer = true to not auto-start them.
    baseURL: 'http://localhost:4200',

    // Capture trace on first retry for debugging
    trace: 'on-first-retry',

    // Screenshot on failure
    screenshot: 'only-on-failure',

    // Video on failure
    video: 'retain-on-failure',

    // Timeout for each action (click, fill, etc.)
    actionTimeout: 10_000,

    // Navigation timeout
    navigationTimeout: 30_000,
  },

  // Timeout for each test
  timeout: 120_000,

  // Expect timeout for assertions
  expect: {
    timeout: 10_000,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  // Do NOT use webServer — both services are expected to already be running.
  // To run tests in CI, start them as part of your CI pipeline before playwright test.
});
