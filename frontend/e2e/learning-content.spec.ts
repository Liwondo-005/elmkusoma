import { test, expect } from "@playwright/test"

const STUDENT_EMAIL = "audit-test@test.com"
const STUDENT_PASSWORD = "password"

async function login(page: import("@playwright/test").Page) {
  await page.goto("/login")
  const emailInput = page.locator("input[type='email'], input[name='email'], [placeholder*='email' i]").first()
  const passwordInput = page.locator("input[type='password'], input[name='password'], [placeholder*='password' i]").first()
  await emailInput.fill(STUDENT_EMAIL)
  await passwordInput.fill(STUDENT_PASSWORD)
  const submitBtn = page.locator("button[type='submit'], button:has-text('Sign in'), button:has-text('Login')").first()
  await submitBtn.click()
  await page.waitForTimeout(3000)
}

test.describe("Learning Content (Resources & Videos)", () => {
  test("learner resources page loads", async ({ page }) => {
    await login(page)
    await page.goto("/dashboard/learner/resources")
    await page.waitForTimeout(4000)
    const body = await page.textContent("body")
    expect(body).not.toContain("Application error")
    expect(body).not.toContain("Failed to load")
    expect(body).not.toContain("Unhandled Runtime")
    expect(body).toContain("Test Resource")
  })

  test("learner video library loads video tutorials", async ({ page }) => {
    await login(page)
    await page.goto("/dashboard/learner/video-library")
    await page.waitForTimeout(4000)
    const body = await page.textContent("body")
    expect(body).not.toContain("Application error")
    expect(body).not.toContain("Unhandled Runtime")
    expect(body).toContain("Video Library")
    expect(body).toContain("Test Video Tutorial")
  })

  test("resource detail shows annotations section", async ({ page }) => {
    await login(page)
    await page.goto("/dashboard/learner/resources")
    await page.waitForTimeout(3000)
    const firstCard = page.locator("a[href^='/dashboard/learner/resources/']").first()
    await firstCard.locator("h3").click()
    await page.waitForURL(/\/dashboard\/learner\/resources\/[0-9a-f-]+/, { timeout: 15000 })
    await page.waitForTimeout(4000)
    const body = await page.textContent("body")
    expect(body).not.toContain("Application error")
    expect(body).toContain("Annotations")
    expect(body).toContain("Add annotation")

    const note = `E2E note ${Date.now()}`
    await page.locator("textarea[aria-label='Annotation content']").fill(note)
    await page.locator("button:has-text('Add annotation')").click()
    await page.waitForTimeout(3000)
    const after = await page.textContent("body")
    expect(after).toContain(note)

    const deleteBtn = page.locator(`button[aria-label='Delete annotation']`).first()
    await deleteBtn.click()
    await page.waitForTimeout(2000)
    const finalBody = await page.textContent("body")
    expect(finalBody).not.toContain(note)
  })
})
