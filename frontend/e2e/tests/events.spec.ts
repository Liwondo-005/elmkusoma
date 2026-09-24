import { test, expect } from "@playwright/test"

test.describe("Events", () => {
  test("events list redirects to login when not authenticated", async ({ page }) => {
    await page.goto("/dashboard/learner/events")
    await expect(page).toHaveURL(/login/)
  })

  test("registered events redirects to login when not authenticated", async ({ page }) => {
    await page.goto("/dashboard/learner/events/registered")
    await expect(page).toHaveURL(/login/)
  })

  test("event detail route requires auth", async ({ page }) => {
    await page.goto("/dashboard/learner/events/00000000-0000-0000-0000-000000000001")
    await expect(page).toHaveURL(/login/)
  })

  test("event preflight requires auth", async ({ page }) => {
    await page.goto("/dashboard/learner/events/00000000-0000-0000-0000-000000000001/preflight")
    await expect(page).toHaveURL(/login/)
  })

  test("event waiting room requires auth", async ({ page }) => {
    await page.goto("/dashboard/learner/events/00000000-0000-0000-0000-000000000001/waiting")
    await expect(page).toHaveURL(/login/)
  })
})
