import { test, expect } from "@playwright/test"

test.describe("Authentication Flow", () => {
  test("landing page loads", async ({ page }) => {
    await page.goto("/")
    await expect(page).toHaveTitle(/ELMKUSOMA/)
  })

  test("login page renders", async ({ page }) => {
    await page.goto("/login")
    await expect(page.locator("text=Sign in")).toBeVisible()
    await expect(page.locator("input[type='email'], input[name='email'], [placeholder*='email' i]")).toBeVisible()
    await expect(page.locator("input[type='password'], input[name='password']")).toBeVisible()
  })

  test("register page renders", async ({ page }) => {
    await page.goto("/register")
    await expect(page.locator("text=Create")).toBeVisible()
  })

  test("login with invalid credentials shows error", async ({ page }) => {
    await page.goto("/login")
    const emailInput = page.locator("input[type='email'], input[name='email'], [placeholder*='email' i]").first()
    const passwordInput = page.locator("input[type='password'], input[name='password']").first()
    await emailInput.fill("nonexistent@example.com")
    await passwordInput.fill("wrongpassword")
    const submitBtn = page.locator("button[type='submit'], button:has-text('Sign in'), button:has-text('Login')").first()
    await submitBtn.click()
    await expect(page.locator("text=Invalid|error|incorrect|failed", { timeout: 10000 })).toBeVisible()
  })
})
