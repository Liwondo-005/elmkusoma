import { test, expect } from "@playwright/test"

test.describe("Login two-panel redesign", () => {
  test("renders both panels, background, and idle status", async ({ page }) => {
    await page.goto("/login")
    await expect(page.getByRole("heading", { name: "Welcome Back" })).toBeVisible()
    await expect(page.getByRole("heading", { name: "Secure Access" })).toBeVisible()
    await expect(page.locator("div[style*='login-bg.jpg']")).toHaveCount(1)
    await expect(page.getByText("Ready to sign you in")).toBeVisible()
    await expect(page.locator("ol[aria-label]")).toBeVisible()
    await expect(page.getByRole("link", { name: /forgot password/i })).toBeVisible()
    await expect(page.getByRole("link", { name: /register/i })).toBeVisible()
  })

  test("empty submit shows validation errors and panel stays idle", async ({ page }) => {
    await page.goto("/login")
    await page.getByRole("button", { name: "Sign In" }).click()
    await expect(page.getByText("Email is required")).toBeVisible()
    await expect(page.getByText("Email is required")).toHaveAttribute("role", "alert")
    await expect(page.getByText("Ready to sign you in")).toBeVisible()
    await expect(page).toHaveURL(/\/login/)
  })

  test("password toggle flips input type and aria-pressed", async ({ page }) => {
    await page.goto("/login")
    const pwd = page.locator("#password")
    const toggle = page.getByRole("button", { name: "Toggle password visibility" })
    await pwd.fill("secret123")
    await expect(pwd).toHaveAttribute("type", "password")
    await expect(toggle).toHaveAttribute("aria-pressed", "false")
    await toggle.click()
    await expect(pwd).toHaveAttribute("type", "text")
    await expect(toggle).toHaveAttribute("aria-pressed", "true")
    await toggle.click()
    await expect(pwd).toHaveAttribute("type", "password")
  })

  test("invalid credentials show real failure state, button re-enabled", async ({ page }) => {
    await page.goto("/login")
    await page.locator("#email").fill("audit-test@test.com")
    await page.locator("#password").fill("wrongpassword")
    await page.getByRole("button", { name: "Sign In" }).click()
    await expect(page.getByText(/couldn't sign you in/i)).toBeVisible({ timeout: 60000 })
    await expect(page.getByText(/check your details/i)).toBeVisible()
    await expect(page.getByRole("button", { name: "Sign In" })).toBeEnabled()
    await expect(page).toHaveURL(/\/login/)
  })

  test("successful login redirects to dashboard", async ({ page }) => {
    await page.goto("/login")
    await page.locator("#email").fill("audit-test@test.com")
    await page.locator("#password").fill("password")
    await page.getByRole("button", { name: "Sign In" }).click()
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 60000 })
  })

  test("mobile: login form above secure panel, button in first viewport", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 })
    await page.goto("/login")
    const form = await page.getByRole("heading", { name: "Welcome Back" }).boundingBox()
    const panel = await page.getByRole("heading", { name: "Secure Access" }).boundingBox()
    expect(form).not.toBeNull()
    expect(panel).not.toBeNull()
    expect(form!.y).toBeLessThan(panel!.y)
    expect(form!.y).toBeLessThan(812)
    await expect(page.getByRole("button", { name: "Sign In" })).toBeVisible()
  })
})
