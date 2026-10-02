import { test, expect, type Page } from "@playwright/test"

// Regional Administration workspace evidence (PROMPT §1-92): the regional shell,
// server-scoped real data, deep navigation and the edge proxy's role gate.
// Nationaladmin.md §8 moved the Regional Admin login landing to /oversight;
// the regional command center stays reachable by deep link and keeps its shell.

const REGIONAL = { email: "regional.dar@test.com", password: "password" }
const DISTRICT = { email: "district.ila@test.com", password: "password" }
const STUDENT = { email: "student1@darms.edu.tz", password: "password" }

async function login(page: Page, email: string, password: string) {
  await page.goto("/login")
  await page.locator("#email").fill(email)
  await page.locator("#password").fill(password)
  await page.locator('button[type="submit"]').click()
}

test("regional admin login lands on the Education Oversight command center", async ({ page }) => {
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await expect(page.getByRole("heading", { name: /Education Oversight/ })).toBeVisible()

  // Exactly one workspace sidebar, and it is the authority shell.
  await expect(page.locator("aside.fixed:visible")).toHaveCount(1)
  await expect(page.locator('aside.fixed a[href^="/dashboard/learner"]')).toHaveCount(0)
  await expect(page.locator('aside.fixed a[href^="/dashboard/platform-admin"]')).toHaveCount(0)
})

test("regional command center stays reachable by deep link with jurisdiction scope", async ({ page }) => {
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await page.goto("/dashboard/regional-admin")
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
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await page.goto("/dashboard/regional-admin/districts")
  await page.waitForURL("**/dashboard/regional-admin/districts", { timeout: 20000 })
  await expect(page.getByRole("heading", { name: "Districts", exact: true })).toBeVisible()
  await expect(page.getByText("Ilala").first()).toBeVisible({ timeout: 15000 })

  await page.locator('a[href*="/dashboard/regional-admin/districts/"]').first().click()
  await page.waitForURL(/\/dashboard\/regional-admin\/districts\/[0-9a-f-]+/, { timeout: 20000 })
  await expect(page.locator("aside.fixed:visible")).toHaveCount(1)
})

test("institutions page shows real schools with jurisdiction-scoped search", async ({ page }) => {
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await page.goto("/dashboard/regional-admin/institutions")
  await page.waitForURL("**/dashboard/regional-admin/institutions", { timeout: 20000 })
  await expect(page.getByRole("heading", { name: "Schools & Institutions" })).toBeVisible()

  // Real seeded institution from this jurisdiction.
  await expect(page.getByText("Test Primary School Ilala").first()).toBeVisible({ timeout: 15000 })

  // Scope to the list's own search form — the top bar has a global search input too.
  const search = page.locator('form[role="search"] input[type="search"]')
  await search.fill("Kinondoni")
  await search.press("Enter")
  await expect(page.getByText("Test Secondary School Kinondoni").first()).toBeVisible({ timeout: 15000 })
  // Scope check: the Arusha institution must never appear for a Dar user.
  await expect(page.getByText("Test Primary School Arusha")).toHaveCount(0)
})

test("governance data-quality page renders jurisdiction-scoped results", async ({ page }) => {
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await page.goto("/dashboard/regional-admin/governance/data-quality")
  await page.waitForURL("**/governance/data-quality", { timeout: 20000 })
  await expect(page.getByRole("heading", { name: /Data Quality/ })).toBeVisible()
  // Real API-backed jurisdiction data — the stat grid only renders once the
  // scoped data-quality response has loaded (fresh fixtures may be clean).
  await expect(page.getByText("Total issues")).toBeVisible({ timeout: 15000 })
})

test("jurisdiction search page returns only in-scope records", async ({ page }) => {
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await page.goto("/dashboard/regional-admin/search?q=Ilala")
  await page.waitForURL("**/regional-admin/search*", { timeout: 20000 })
  await expect(page.getByRole("heading", { name: "Search", exact: true })).toBeVisible()
  await expect(page.getByText("Test Primary School Ilala").first()).toBeVisible({ timeout: 15000 })
  await expect(page.getByText("Test Primary School Arusha")).toHaveCount(0)
})

test("district admin can open the regional workspace for their district", async ({ page }) => {
  await login(page, DISTRICT.email, DISTRICT.password)
  // Workspace resolution lands District Admins on their own oversight
  // dashboard; the regional command center stays reachable by deep link.
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await page.goto("/dashboard/regional-admin")
  await page.waitForURL("**/dashboard/regional-admin", { timeout: 25000 })
  await expect(page.getByRole("heading", { name: /Regional Education Command Center/ })).toBeVisible()
  await expect(page.getByText("Ilala").first()).toBeVisible({ timeout: 15000 })
})

test("wards page shows real ward geography with jurisdiction-scoped search", async ({ page }) => {
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await page.goto("/dashboard/regional-admin/wards")
  await page.waitForURL("**/regional-admin/wards", { timeout: 20000 })
  await expect(page.getByRole("heading", { name: "Wards", exact: true })).toBeVisible()
  await expect(page.locator('[data-testid="wards-page"]')).toBeVisible()

  // Real seeded wards of this jurisdiction (Ilala + Kinondoni).
  await expect(page.getByText("Kariakoo").first()).toBeVisible({ timeout: 15000 })
  await expect(page.getByText("Mchikichini").first()).toBeVisible({ timeout: 15000 })

  // Scope check: an Arusha ward must never appear for a Dar user.
  // Scope to the list's own search form — the top bar has a global search input too.
  const search = page.locator('form[role="search"] input[type="search"]')
  await search.fill("Njiro")
  await search.press("Enter")
  await expect(page.getByText("No wards found")).toBeVisible({ timeout: 15000 })
  await expect(page.getByText("Njiro")).toHaveCount(0)

  await search.fill("")
  await search.press("Enter")
  await page.locator('a[href*="/dashboard/regional-admin/wards/"]').first().click()
  await page.waitForURL(/\/dashboard\/regional-admin\/wards\/[0-9a-f-]+/, { timeout: 20000 })
  await expect(page.locator('[data-testid="ward-detail-page"]')).toBeVisible()
})

test("scheduled reports can be created, run and deleted with real snapshots", async ({ page }) => {
  await login(page, REGIONAL.email, REGIONAL.password)
  await page.waitForURL("**/oversight", { timeout: 25000 })

  await page.goto("/dashboard/regional-admin/scheduled-reports")
  await page.waitForURL("**/regional-admin/scheduled-reports", { timeout: 20000 })
  await expect(page.getByRole("heading", { name: "Scheduled Reports" })).toBeVisible()

  const title = `E2E governance digest ${Date.now()}`
  const form = page.locator('[data-testid="scheduled-reports-page"] form').first()
  await form.locator('input[placeholder="e.g. Monthly attendance digest"]').fill(title)
  await form.locator('select').first().selectOption("GOVERNANCE")
  await form.locator('select').nth(1).selectOption("DAILY")
  await form.getByRole("button", { name: "Create report" }).click()

  // New card appears with its jurisdiction and schedule.
  await expect(page.getByText(title)).toBeVisible({ timeout: 15000 })
  const card = page.locator("div.rounded-2xl.border", { hasText: title }).first()
  await expect(card.getByText("Dar es Salaam")).toBeVisible()

  // Run it now — the run history must show a real SUCCESS snapshot.
  await card.getByRole("button", { name: "Run now" }).click()
  await expect(card.getByText("SUCCESS").first()).toBeVisible({ timeout: 15000 })
  await expect(card.getByText(/"institutions"/).first()).toBeVisible({ timeout: 15000 })

  // Clean up so the fixture stays reusable across runs.
  await page.getByRole("button", { name: `Delete ${title}` }).click()
  await expect(page.getByText(title)).toHaveCount(0, { timeout: 15000 })
})

test("proxy bounces non-regional roles away from the regional workspace", async ({ page }) => {
  await login(page, STUDENT.email, STUDENT.password)
  await page.waitForURL((u) => u.pathname.startsWith("/dashboard"), { timeout: 25000 })

  const resp = await page.request.get("/dashboard/regional-admin", { maxRedirects: 0 })
  expect([301, 302, 303, 307, 308]).toContain(resp.status())
  expect(resp.headers()["location"] || "").toContain("/dashboard")
})
