import { test, expect, type Page } from "@playwright/test"

// Regional Administration workspace evidence (PROMPT §1-92): the regional shell,
// server-scoped real data, deep navigation and the edge proxy's role gate.

const REGIONAL = { email: "regional.dar@test.com", password: "password" }
const DISTRICT = { email: "district.ila@test.com", password: "password" }
const STUDENT = { email: "shamsa@gmail.com", password: "password" }

async function login(page: Page, email: string, password: string) {
  await page.goto("/login")
  await page.locator("#email").fill(email)
  await page.locator("#password").fill(password)
  await page.locator('button[type="submit"]').click()
}

test("regional admin login lands on the Regional Education Command Center", async ({ page }) => {
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/dashboard/regional-admin", { timeout: 25000 })

  await expect(page.getByRole("heading", { name: /Regional Education Command Center/ })).toBeVisible()

  // Exactly one workspace sidebar, and it belongs to the regional shell.
  await expect(page.locator("aside.fixed:visible")).toHaveCount(1)
  await expect(page.locator('aside.fixed a[href="/dashboard/regional-admin"]').first()).toBeVisible()

  // Real jurisdiction chip resolved server-side for this account.
  await expect(page.getByText(/Dar es Salaam/).first()).toBeVisible({ timeout: 15000 })

  // No learner shell links leaking into the regional sidebar.
  await expect(page.locator('aside.fixed a[href^="/dashboard/learner"]')).toHaveCount(0)
  await expect(page.locator('aside.fixed a[href^="/dashboard/platform-admin"]')).toHaveCount(0)
})

test("districts page lists real jurisdiction districts and opens a detail", async ({ page }) => {
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/dashboard/regional-admin", { timeout: 25000 })

  await page.locator('aside.fixed a[href="/dashboard/regional-admin/districts"]').click()
  await page.waitForURL("**/dashboard/regional-admin/districts", { timeout: 20000 })
  await expect(page.getByRole("heading", { name: "Districts", exact: true })).toBeVisible()
  await expect(page.getByText("Ilala").first()).toBeVisible({ timeout: 15000 })

  await page.locator('a[href*="/dashboard/regional-admin/districts/"]').first().click()
  await page.waitForURL(/\/dashboard\/regional-admin\/districts\/[0-9a-f-]+/, { timeout: 20000 })
  await expect(page.locator("aside.fixed:visible")).toHaveCount(1)
})

test("institutions page shows real schools with jurisdiction-scoped search", async ({ page }) => {
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/dashboard/regional-admin", { timeout: 25000 })

  await page.goto("/dashboard/regional-admin/institutions")
  await page.waitForURL("**/dashboard/regional-admin/institutions", { timeout: 20000 })
  await expect(page.getByRole("heading", { name: "Schools & Institutions" })).toBeVisible()

  // Real seeded institution from this jurisdiction.
  await expect(page.getByText("Test Primary School Ilala").first()).toBeVisible({ timeout: 15000 })

  const search = page.locator('input[type="search"]').first()
  await search.fill("Kinondoni")
  await search.press("Enter")
  await expect(page.getByText("Test Secondary School Kinondoni").first()).toBeVisible({ timeout: 15000 })
  // Scope check: the Arusha institution must never appear for a Dar user.
  await expect(page.getByText("Test Primary School Arusha")).toHaveCount(0)
})

test("governance data-quality page surfaces a real finding", async ({ page }) => {
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/dashboard/regional-admin", { timeout: 25000 })

  await page.goto("/dashboard/regional-admin/governance/data-quality")
  await page.waitForURL("**/governance/data-quality", { timeout: 20000 })
  await expect(page.getByRole("heading", { name: /Data Quality/ })).toBeVisible()
  await expect(page.getByText(/Dar es Salaam Model School/).first()).toBeVisible({ timeout: 15000 })
})

test("jurisdiction search page returns only in-scope records", async ({ page }) => {
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/dashboard/regional-admin", { timeout: 25000 })

  await page.goto("/dashboard/regional-admin/search?q=Ilala")
  await page.waitForURL("**/regional-admin/search*", { timeout: 20000 })
  await expect(page.getByRole("heading", { name: "Search", exact: true })).toBeVisible()
  await expect(page.getByText("Test Primary School Ilala").first()).toBeVisible({ timeout: 15000 })
  await expect(page.getByText("Test Primary School Arusha")).toHaveCount(0)
})

test("district admin can open the regional workspace for their district", async ({ page }) => {
  await login(page, DISTRICT.email, DISTRICT.password)
  // Workspace resolution lands District Admins on their own district oversight
  // dashboard; the regional command center stays reachable by deep link.
  await page.waitForURL((u) => u.pathname.startsWith("/dashboard"), { timeout: 25000 })

  await page.goto("/dashboard/regional-admin")
  await page.waitForURL("**/dashboard/regional-admin", { timeout: 25000 })
  await expect(page.getByRole("heading", { name: /Regional Education Command Center/ })).toBeVisible()
  await expect(page.getByText("Ilala").first()).toBeVisible({ timeout: 15000 })
})

test("proxy bounces non-regional roles away from the regional workspace", async ({ page }) => {
  await login(page, STUDENT.email, STUDENT.password)
  await page.waitForURL((u) => u.pathname.startsWith("/dashboard"), { timeout: 25000 })

  const resp = await page.request.get("/dashboard/regional-admin", { maxRedirects: 0 })
  expect([301, 302, 303, 307, 308]).toContain(resp.status())
  expect(resp.headers()["location"] || "").toContain("/dashboard")
})
