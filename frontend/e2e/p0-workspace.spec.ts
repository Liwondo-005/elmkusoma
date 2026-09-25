import { test, expect, type Page } from "@playwright/test"

// P0 workspace isolation evidence (spec §10): platform-admin pages must never be
// wrapped by the learner shell, workspace resolution must route by role, and
// proxy + page guards must keep non-admins out.

const ADMIN = { email: "admin@elmkusoma.go.tz", password: "password" }
const INST_ADMIN = { email: "admin@darms.edu.tz", password: "password" }
const STUDENT = { email: "john@student.test", password: "password" }

async function login(page: Page, email: string, password: string) {
  await page.goto("/login")
  await page.locator("#email").fill(email)
  await page.locator("#password").fill(password)
  await page.locator('button[type="submit"]').click()
}

async function waitForOwnDashboard(page: Page) {
  // Any authenticated /dashboard/* landing EXCEPT the platform-admin workspace.
  await page.waitForURL(
    (u) => u.pathname.startsWith("/dashboard") && !u.pathname.includes("platform-admin"),
    { timeout: 20000 },
  )
}

async function assertProxyBounces(page: Page, from: string) {
  // Request-level check: the edge proxy must redirect non-admins away from
  // admin-only routes BEFORE any page JS runs (redirects not followed).
  const resp = await page.request.get(from, { maxRedirects: 0 })
  expect([301, 302, 303, 307, 308]).toContain(resp.status())
  expect(resp.headers()["location"] || "").toContain("/dashboard")
}

test("admin login resolves to platform-admin workspace with a single sidebar", async ({ page }) => {
  await login(page, ADMIN.email, ADMIN.password)
  await page.waitForURL("**/dashboard/platform-admin", { timeout: 20000 })

  // Exactly one fixed sidebar aside — the learner shell must not wrap this page.
  await expect(page.locator("aside.fixed:visible")).toHaveCount(1)
  await expect(page.locator('aside.fixed a[href="/dashboard/platform-admin"]').first()).toBeVisible()

  // No learner/secondary workspace links leaked into the sidebar.
  await expect(page.locator('aside.fixed a[href^="/dashboard/learner"]')).toHaveCount(0)
  await expect(page.locator('aside.fixed a[href^="/dashboard/secondary"]')).toHaveCount(0)
})

test("platform-admin deep link renders its own shell", async ({ page }) => {
  await login(page, ADMIN.email, ADMIN.password)
  await page.waitForURL("**/dashboard/platform-admin", { timeout: 20000 })

  await page.goto("/dashboard/platform-admin/users")
  await page.waitForURL("**/dashboard/platform-admin/users", { timeout: 20000 })
  await expect(page.locator("aside.fixed:visible")).toHaveCount(1)
  await expect(page.locator('aside.fixed a[href="/dashboard/platform-admin/admins"]').first()).toBeVisible()
  await expect(page.locator('aside.fixed a[href^="/dashboard/learner"]')).toHaveCount(0)
})

test("mobile drawer renders the platform-admin sidebar and closes on navigate", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await login(page, ADMIN.email, ADMIN.password)
  await page.waitForURL("**/dashboard/platform-admin", { timeout: 20000 })

  // Hamburger is the first header button on mobile.
  await page.locator("header button").first().click()
  // PlatformAdminSidebar footer marker (only the open drawer is visible at 390px).
  await expect(page.locator("p:visible", { hasText: "Platform Status" })).toHaveCount(1)
  await expect(page.locator('a[href^="/dashboard/learner"]:visible')).toHaveCount(0)
  await expect(page.locator('a[href^="/dashboard/secondary"]:visible')).toHaveCount(0)

  // onNavigate wiring: choosing an item navigates and closes the drawer.
  await page.locator('a[href="/dashboard/platform-admin/attention"]:visible').first().click()
  await page.waitForURL("**/dashboard/platform-admin/attention", { timeout: 20000 })
  await expect(page.locator("p:visible", { hasText: "Platform Status" })).toHaveCount(0)
})

test("student is bounced from the platform-admin workspace", async ({ page }) => {
  await login(page, STUDENT.email, STUDENT.password)
  await waitForOwnDashboard(page)

  // Proxy-level bounce (server-side, before any page JS).
  await assertProxyBounces(page, "/dashboard/platform-admin")

  await page.goto("/dashboard/platform-admin")
  await waitForOwnDashboard(page)
  expect(new URL(page.url()).pathname).not.toContain("/dashboard/platform-admin")
  // Never saw platform-admin nav content.
  await expect(page.locator('a[href="/dashboard/platform-admin/admins"]')).toHaveCount(0)
})

test("institution admin is excluded from platform-admin and sees filtered admin nav", async ({ page }) => {
  await login(page, INST_ADMIN.email, INST_ADMIN.password)
  await waitForOwnDashboard(page)

  // Proxy-level bounce (server-side, before any page JS).
  await assertProxyBounces(page, "/dashboard/platform-admin")

  await page.goto("/dashboard/platform-admin")
  await waitForOwnDashboard(page)
  expect(new URL(page.url()).pathname).not.toContain("/dashboard/platform-admin")
  await page.waitForURL("**/dashboard/admin", { timeout: 20000 })

  // Administration nav present, platform-admin entry filtered, no learner nav.
  await expect(page.locator("aside.fixed:visible")).toHaveCount(1)
  await expect(page.locator('aside.fixed a[href="/dashboard/admin/people"]')).toBeVisible()
  await expect(page.locator('aside.fixed a[href="/dashboard/platform-admin"]')).toHaveCount(0)
  await expect(page.locator('aside.fixed a[href^="/dashboard/learner"]')).toHaveCount(0)
})

test("platform admin on /dashboard/admin sees administration nav only", async ({ page }) => {
  await login(page, ADMIN.email, ADMIN.password)
  await page.waitForURL("**/dashboard/platform-admin", { timeout: 20000 })

  await page.goto("/dashboard/admin")
  await page.waitForURL("**/dashboard/admin", { timeout: 20000 })
  await expect(page.locator("aside.fixed:visible")).toHaveCount(1)
  await expect(page.locator('aside.fixed a[href="/dashboard/admin/people"]')).toBeVisible()
  await expect(page.locator('aside.fixed a[href="/dashboard/platform-admin"]')).toHaveCount(1)
  await expect(page.locator('aside.fixed a[href^="/dashboard/learner"]')).toHaveCount(0)
  await expect(page.locator('aside.fixed a[href^="/dashboard/secondary"]')).toHaveCount(0)
})

test("admin passes the live-operations role guard (no bounce)", async ({ page }) => {
  await login(page, ADMIN.email, ADMIN.password)
  await page.waitForURL("**/dashboard/platform-admin", { timeout: 20000 })

  await page.goto("/dashboard/admin/live-operations")
  await page.waitForURL("**/dashboard/admin/live-operations", { timeout: 20000 })
  // Client-side guard effect runs on mount; the admin must NOT be bounced.
  await page.waitForTimeout(2500)
  expect(new URL(page.url()).pathname).toBe("/dashboard/admin/live-operations")
})

test("student is blocked from live-operations", async ({ page }) => {
  await login(page, STUDENT.email, STUDENT.password)
  await waitForOwnDashboard(page)

  // Proxy-level bounce for admin-only routes.
  await assertProxyBounces(page, "/dashboard/admin/live-operations")

  await page.goto("/dashboard/admin/live-operations")
  await page.waitForLoadState("domcontentloaded")
  await page.waitForTimeout(2000)
  expect(new URL(page.url()).pathname).not.toContain("/dashboard/admin/live-operations")
})
