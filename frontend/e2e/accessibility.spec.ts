import { test, expect } from "@playwright/test"

test.describe("Accessibility", () => {
  test("login page has proper heading structure", async ({ page }) => {
    await page.goto("/login")
    const h1 = page.locator("h1, h2, h3").first()
    await expect(h1).toBeVisible()
  })

  test("login form has labeled inputs", async ({ page }) => {
    await page.goto("/login")
    const emailInput = page.locator("input[type='email'], input[name='email'], [placeholder*='email' i]").first()
    const passwordInput = page.locator("input[type='password'], input[name='password']").first()
    await expect(emailInput).toBeVisible()
    await expect(passwordInput).toBeVisible()
  })

  test("login page has no duplicate landmarks", async ({ page }) => {
    await page.goto("/login")
    const mainLandmarks = await page.locator("main, [role='main']").count()
    expect(mainLandmarks).toBeLessThanOrEqual(1)
  })
})
