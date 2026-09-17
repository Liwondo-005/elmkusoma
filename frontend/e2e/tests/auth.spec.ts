import { test, expect } from "@playwright/test"

test.describe("Authentication", () => {
  test("should display login page", async ({ page }) => {
    await page.goto("/login")
    await expect(page).toHaveTitle(/ELMKUSOMA/)
  })

  test("should show login form elements", async ({ page }) => {
    await page.goto("/login")
    await expect(page.locator("input[type='email'], input[name='email']")).toBeVisible()
    await expect(page.locator("input[type='password'], input[name='password']")).toBeVisible()
  })

  test("should navigate to register page", async ({ page }) => {
    await page.goto("/login")
    const registerLink = page.locator("a[href='/register']")
    if (await registerLink.isVisible()) {
      await registerLink.click()
      await expect(page).toHaveURL(/.*register/)
    }
  })
})
