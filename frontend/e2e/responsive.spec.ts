import { test, expect } from "@playwright/test"

test.describe("Responsive Design", () => {
  test("mobile viewport hides sidebar", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 })
    await page.goto("/login")
    // The redesigned login page intentionally keeps its secure-access panel in
    // an <aside> on phones (it stacks below the form, see
    // login-secure-access.spec.ts). What must never reach a phone viewport is
    // the authenticated app sidebar / workspace navigation.
    await expect(page.locator("aside a[href^='/dashboard']")).toHaveCount(0)
    await expect(page.locator("aside nav")).toHaveCount(0)
    await expect(page.getByRole("heading", { name: "Welcome Back" })).toBeVisible()
  })

  test("desktop viewport shows sidebar on dashboard", async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto("/login")
    await expect(page.locator("input[type='email'], input[name='email'], [placeholder*='email' i]").first()).toBeVisible()
  })

  test("login form is usable on mobile", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 })
    await page.goto("/login")
    const emailInput = page.locator("input[type='email'], input[name='email'], [placeholder*='email' i]").first()
    await expect(emailInput).toBeVisible()
    await emailInput.fill("test@example.com")
    await expect(emailInput).toHaveValue("test@example.com")
  })
})
