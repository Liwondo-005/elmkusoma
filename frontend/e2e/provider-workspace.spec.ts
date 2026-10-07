import { test, expect } from "@playwright/test"

const BASE = "http://localhost:3000"
const API = "http://localhost:8080"
const PROVIDER_EMAIL = "provider-admin@arushatc-e2e.test"
const PROVIDER_PASSWORD = "password"
// A institution the logged-in provider admin has no membership in (seeded E2E data).
const FOREIGN_INSTITUTION = "11111111-1111-1111-1111-111111111111"

async function apiLogin(page, email, password) {
  const res = await page.request.post(`${API}/v1/auth/login`, {
    data: { email, password },
  })
  const body = await res.json()
  expect(body.success).toBeTruthy()
  const token = body.data.accessToken
  const user = body.data.user
  // Mirror lib/auth.tsx mapRoleToFrontend: the app stores display roles.
  const roleMap = {
    STUDENT: "Student",
    TEACHER: "Teacher",
    PARENT: "Parent",
    OTHER_LEARNER: "Other Learner",
    ADMIN: "Admin",
    INSTITUTION_ADMIN: "Institution Admin",
    PROVIDER_ADMIN: "Provider Admin",
    PROVIDER_STAFF: "Provider Staff",
  }
  user.role = roleMap[user.role] ?? user.role
  const enc = encodeURIComponent(JSON.stringify(user))
  await page.context().addCookies([
    { name: "elmkusoma_access_token", value: token, url: BASE },
    { name: "elmkusoma_current_user", value: enc, url: BASE },
    { name: "elmkusoma_institution_id", value: user.institutionId ?? "", url: BASE },
  ])
  await page.addInitScript(
    ({ t, u, i }) => {
      localStorage.setItem("elmkusoma_access_token", t)
      localStorage.setItem("elmkusoma_current_user", u)
      if (i) localStorage.setItem("elmkusoma_institution_id", i)
    },
    { t: token, u: JSON.stringify(user), i: user.institutionId ?? "" }
  )
  return { token, user }
}

function trackMissing(page): string[] {
  const missing: string[] = []
  page.on("console", (m) => {
    if (m.type() === "error" && m.text().includes("MISSING_MESSAGE")) missing.push(m.text())
  })
  return missing
}

test.describe("Provider workspace", () => {
  test("provider admin sees the live provider dashboard and workspace navigation", async ({ page }) => {
    test.setTimeout(120000)
    const missing = trackMissing(page)
    await apiLogin(page, PROVIDER_EMAIL, PROVIDER_PASSWORD)

    await page.goto(`${BASE}/dashboard/provider`, { waitUntil: "domcontentloaded" })
    await expect(page.getByRole("heading", { name: "Provider Dashboard", level: 1 })).toBeVisible({ timeout: 30000 })
    // Stats come from /v1/nfe/providers/stats — no hardcoded numbers on this page.
    await expect(page.getByText("Manage your non-formal education programs")).toBeVisible()

    // Workspace nav (sidebar renders a <Link> per item; desktop viewport ≥ lg).
    for (const label of ["Programs", "Sessions", "Learners", "Materials", "Assessments", "Attendance", "Certificates", "Settings"]) {
      await expect(page.getByRole("link", { name: label }).first()).toBeVisible()
    }

    await page.getByRole("link", { name: "Programs" }).first().click()
    await expect(page).toHaveURL(/\/dashboard\/provider\/programs/)
    await expect(page.getByRole("heading", { name: "Programs", level: 1 })).toBeVisible({ timeout: 30000 })
    await expect(page.getByText("Manage your NFE programs")).toBeVisible()

    expect(missing).toEqual([])
  })

  test("program lifecycle: create, prefilled edit, armed two-click delete", async ({ page }) => {
    test.setTimeout(120000)
    const missing = trackMissing(page)
    await apiLogin(page, PROVIDER_EMAIL, PROVIDER_PASSWORD)

    await page.goto(`${BASE}/dashboard/provider/programs`, { waitUntil: "domcontentloaded" })
    await expect(page.getByRole("heading", { name: "Programs", level: 1 })).toBeVisible({ timeout: 30000 })

    const stamp = Date.now()
    const title = `E2E PW Program ${stamp}`
    // Distinct prefix so hasText substring matching cannot conflate the two.
    const edited = `E2E PW Program v2 ${stamp}`
    const row = (text: string) => page.locator("div.rounded-lg.border", { hasText: text })

    try {
      // --- create ---
      await page.getByRole("button", { name: "Create Program" }).click()
      const dialog = page.getByRole("dialog")
      await expect(dialog).toBeVisible()
      await page.locator("#pcd-title").fill(title)
      // Type* is required and defaults to the empty "Select..." option.
      await dialog.getByRole("combobox", { name: "Type*" }).selectOption("COURSE")
      // In create mode the submit button inherits the dialog title.
      await dialog.getByRole("button", { name: "Create Program" }).click()
      await expect(page.getByText("Created successfully.")).toBeVisible({ timeout: 15000 })
      await expect(dialog).toHaveCount(0)
      await expect(row(title)).toHaveCount(1, { timeout: 15000 })

      // --- edit: dialog must prefill the existing program ---
      await row(title).getByRole("button", { name: "Edit" }).click()
      await expect(page.getByRole("dialog")).toBeVisible()
      await expect(page.getByRole("heading", { name: `Edit: ${title}` })).toBeVisible()
      await expect(page.locator("#pcd-title")).toHaveValue(title)
      await page.locator("#pcd-title").fill(edited)
      await page.getByRole("dialog").getByRole("button", { name: "Save" }).click()
      await expect(page.getByText("Changes saved.")).toBeVisible({ timeout: 15000 })
      await expect(page.getByRole("dialog")).toHaveCount(0)
      await expect(row(edited)).toHaveCount(1, { timeout: 15000 })
      await expect(row(title)).toHaveCount(0)

      // --- delete: first click arms, second click confirms ---
      await row(edited).getByRole("button", { name: "Delete" }).click()
      await expect(row(edited).getByRole("button", { name: "Confirm?" })).toBeVisible()
      await row(edited).getByRole("button", { name: "Confirm?" }).click()
      await expect(page.getByText("Deleted.")).toBeVisible({ timeout: 15000 })
      await expect(row(edited)).toHaveCount(0, { timeout: 15000 })
    } finally {
      // Cleanup if the test failed before the delete step (server-side, authoritative).
      const token = await page.evaluate(() => localStorage.getItem("elmkusoma_access_token"))
      const inst = await page.evaluate(() => localStorage.getItem("elmkusoma_institution_id"))
      const headers = { Authorization: `Bearer ${token}`, "X-Institution-Id": inst ?? "" }
      const list = await page.request.get(`${API}/v1/nfe/programs`, { headers })
      if (list.ok()) {
        const body = await list.json()
        const items = Array.isArray(body.data) ? body.data : body.data?.content ?? []
        for (const p of items.filter((x) => String(x.title ?? "").startsWith("E2E PW Program "))) {
          await page.request.delete(`${API}/v1/nfe/programs/${p.id}`, { headers }).catch(() => {})
        }
      }
    }

    expect(missing).toEqual([])
  })

  test("authorization matrix: own scope allowed, foreign scope and platform-admin denied, bad requests map to 400", async ({ page }) => {
    test.setTimeout(120000)
    const { token, user } = await apiLogin(page, PROVIDER_EMAIL, PROVIDER_PASSWORD)
    await page.goto(`${BASE}/dashboard/provider`, { waitUntil: "domcontentloaded" })
    const inst =
      user.institutionId ??
      (await page.evaluate(() => localStorage.getItem("elmkusoma_institution_id"))) ??
      ""
    const own = { Authorization: `Bearer ${token}`, "X-Institution-Id": inst }

    // 1. Own institution search → allowed.
    const search = await page.request.get(`${API}/v1/admin/search?q=e`, { headers: own })
    expect(search.status()).toBe(200)

    // 2. Foreign institution header → denied (server resolves scope; client cannot widen it).
    const foreign = await page.request.get(`${API}/v1/admin/search?q=e`, {
      headers: { ...own, "X-Institution-Id": FOREIGN_INSTITUTION },
    })
    expect(foreign.status()).toBe(403)

    // 3. Platform-admin surface is ADMIN-only → denied for a provider admin.
    const platform = await page.request.get(`${API}/v1/platform-admin/search?q=e`, { headers: own })
    expect(platform.status()).toBe(403)

    // 4. Export of own users → CSV containing this institution only.
    const csv = await page.request.get(`${API}/v1/admin/export?entityType=users`, { headers: own })
    expect(csv.status()).toBe(200)
    expect(csv.headers()["content-type"]).toContain("text/csv")
    const body = await csv.text()
    expect(body).toContain("provider-admin@arushatc-e2e.test")
    expect(body).not.toContain("darms.edu.tz") // belongs to institution ...0002

    // 5. Missing required parameter → 400 with a safe message (was an opaque 500).
    const bad = await page.request.get(`${API}/v1/admin/export`, { headers: own })
    expect(bad.status()).toBe(400)
    const badBody = await bad.json()
    expect(badBody.success).toBe(false)
    expect(badBody.error).toBeTruthy()
  })

  test("provider admin is routed away from the platform-admin workspace", async ({ page }) => {
    test.setTimeout(120000)
    const missing = trackMissing(page)
    await apiLogin(page, PROVIDER_EMAIL, PROVIDER_PASSWORD)

    await page.goto(`${BASE}/dashboard/platform-admin/users`, { waitUntil: "domcontentloaded" })
    // The (platform-admin) layout redirects any non-"Admin" role to the dashboard.
    await expect(page).not.toHaveURL(/\/dashboard\/platform-admin/, { timeout: 30000 })
    await expect(page.getByRole("heading", { name: "Provider Dashboard", level: 1 })).toBeVisible({ timeout: 30000 })

    expect(missing).toEqual([])
  })
})
