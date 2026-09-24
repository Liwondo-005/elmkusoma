import { test, expect } from "@playwright/test"

test.describe("Replays", () => {
  test("replays list redirects to login when not authenticated", async ({ page }) => {
    await page.goto("/dashboard/learner/replays")
    await expect(page).toHaveURL(/login/)
  })

  test("replay viewer requires auth", async ({ page }) => {
    await page.goto("/dashboard/learner/replays/00000000-0000-0000-0000-000000000001")
    await expect(page).toHaveURL(/login/)
  })

  test("media library requires auth", async ({ page }) => {
    await page.goto("/dashboard/learner/media-library")
    await expect(page).toHaveURL(/login/)
  })
})
