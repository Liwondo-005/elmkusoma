import { defineConfig, devices } from "@playwright/test"

// PLAYWRIGHT_BASE_URL lets E2E run against a non-default port when 3000 is
// already serving another local project.
const baseUrl = process.env.PLAYWRIGHT_BASE_URL || "http://localhost:3000"

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  // Specs wait up to 60s for live-video states; the 30s default aborted them first.
  timeout: 180_000,
  reporter: "list",
  use: {
    baseURL: baseUrl,
    trace: "on-first-retry",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  webServer: {
    command: "npm run dev",
    url: baseUrl,
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },
})
