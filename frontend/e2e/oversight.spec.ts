import { test, expect, type Page } from "@playwright/test"

// Nationaladmin.md §7/§39 smoke: the authority workspace landing, governance
// sub-pages, jurisdiction-scoped real data, i18n integrity, and the edge gate
// that keeps non-authority roles out of the command center.

const NATIONAL = { email: "national@elmkusoma.go.tz", password: "password" }
const REGIONAL = { email: "regional@elmkusoma.go.tz", password: "password" }
const LEARNER = { email: "student1@darms.edu.tz", password: "password" }

function trackMissing(page: Page): string[] {
  const missing: string[] = []
  page.on("console", (m) => {
    if (m.type() === "error" && m.text().includes("MISSING_MESSAGE")) missing.push(m.text())
  })
  return missing
}

async function login(page: Page, email: string, password: string) {
  await page.goto("/login")
  await page.locator("#email").fill(email)
  await page.locator("#password").fill(password)
  await page.locator('button[type="submit"]').click()
}

test("national admin lands on the oversight command center", async ({ page }) => {
  const missing = trackMissing(page)
  await login(page, NATIONAL.email, NATIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await expect(
    page.getByRole("heading", { name: "Education Oversight Command Center" }),
  ).toBeVisible({ timeout: 15000 })

  // Exactly one sidebar, and it is the authority shell with live nav targets.
  // (The reports href matches a nav item and a quick-action link → .first().)
  await expect(page.locator("aside.fixed:visible")).toHaveCount(1)
  await expect(page.locator('aside.fixed a[href="/oversight/schools"]')).toBeVisible({ timeout: 15000 })
  await expect(page.locator('aside.fixed a[href="/oversight/reports"]').first()).toBeVisible({ timeout: 15000 })
  await expect(page.locator('aside.fixed a[href^="/dashboard/learner"]')).toHaveCount(0)
  await expect(page.locator('aside.fixed a[href^="/dashboard/platform-admin"]')).toHaveCount(0)

  expect(missing).toEqual([])
})

test("regional admin overview is scoped to their own region", async ({ page }) => {
  const missing = trackMissing(page)
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await expect(
    page.getByRole("heading", { name: "Education Oversight Command Center" }),
  ).toBeVisible({ timeout: 15000 })

  // Server-resolved jurisdiction for this account — Dar only, no Arusha data.
  await expect(page.getByText(/Dar es Salaam/).first()).toBeVisible({ timeout: 15000 })
  await expect(page.getByText("Arusha Technical College")).toHaveCount(0)

  expect(missing).toEqual([])
})

test("reports center renders for the national admin", async ({ page }) => {
  const missing = trackMissing(page)
  await login(page, NATIONAL.email, NATIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await page.goto("/oversight/reports")
  await expect(page.getByRole("heading", { name: "Reports Center" })).toBeVisible({ timeout: 15000 })
  await expect(page.locator("aside.fixed:visible")).toHaveCount(1)

  expect(missing).toEqual([])
})

test("data quality page renders for the national admin", async ({ page }) => {
  const missing = trackMissing(page)
  await login(page, NATIONAL.email, NATIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await page.goto("/oversight/data-quality")
  await expect(page.getByRole("heading", { name: "Data Quality" })).toBeVisible({ timeout: 15000 })

  expect(missing).toEqual([])
})

test("notifications page renders for the national admin", async ({ page }) => {
  const missing = trackMissing(page)
  await login(page, NATIONAL.email, NATIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await page.goto("/oversight/notifications")
  await expect(page.getByRole("heading", { name: "Notifications" })).toBeVisible({ timeout: 15000 })

  expect(missing).toEqual([])
})

test("national admin can open the announcement composer", async ({ page }) => {
  const missing = trackMissing(page)
  await login(page, NATIONAL.email, NATIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await page.goto("/oversight/announcements")
  await expect(page.getByRole("heading", { name: "Announcements" })).toBeVisible({ timeout: 15000 })

  // §24 compose surface is role-gated to National Admin.
  await page.getByRole("button", { name: "New announcement" }).click()
  await expect(page.getByRole("heading", { name: "Compose announcement" })).toBeVisible()

  expect(missing).toEqual([])
})

test("non-authority users are bounced off /oversight at the edge", async ({ page }) => {
  await login(page, LEARNER.email, LEARNER.password)
  await page.waitForURL((u) => u.pathname.startsWith("/dashboard"), { timeout: 25000 })

  const resp = await page.request.get("/oversight", { maxRedirects: 0 })
  expect([301, 302, 303, 307, 308]).toContain(resp.status())
  expect(resp.headers()["location"] || "").toContain("/dashboard")
})

test("anonymous visitors are sent to login from /oversight", async ({ page }) => {
  const resp = await page.request.get("/oversight", { maxRedirects: 0 })
  expect([301, 302, 303, 307, 308]).toContain(resp.status())
  expect(resp.headers()["location"] || "").toContain("/login")
})
