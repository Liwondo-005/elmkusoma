import { test, expect } from "@playwright/test"

test.describe("Navigation", () => {
  test("dashboard redirects to login when not authenticated", async ({ page }) => {
    await page.goto("/dashboard")
    await expect(page).toHaveURL(/login/)
  })

  test("education level selection page loads", async ({ page }) => {
    await page.goto("/")
    await expect(page).toHaveTitle(/ELMKUSOMA/)
  })
})
