import { test, expect } from "@playwright/test"

test.describe("Public Navigation", () => {
  test("should load home page", async ({ page }) => {
    await page.goto("/")
    await expect(page).toHaveTitle(/ELMKUSOMA/)
  })

  test("should navigate to courses", async ({ page }) => {
    await page.goto("/courses")
    await expect(page.locator("h1, h2").first()).toBeVisible()
  })

  test("should navigate to about page", async ({ page }) => {
    await page.goto("/about")
    await expect(page.locator("h1, h2").first()).toBeVisible()
  })

  test("should navigate to live classes", async ({ page }) => {
    await page.goto("/live-classes")
    await expect(page.locator("h1, h2").first()).toBeVisible()
  })
})
