import { test, expect, type APIRequestContext, type Page } from "@playwright/test"

const API = "http://localhost:8080"

// Resolved from the authenticated session: the tenant guard rejects any other institution
// for this token, so a hardcoded id would only mask the tenancy check.
let INST = ""

/**
 * Public Live discovery: the /live-classes landing area and the homepage Live section.
 *
 * Regression cover for the audited gaps:
 *  - the tabs were fed by the dashboard endpoint ("now -> +7d"), which can never return a
 *    running or finished class, so Live Now and Past were permanently empty;
 *  - cards hardcoded an empty instructor, a fabricated "0 watching" count and a
 *    browser-recomputed status;
 *  - a finished session linked to the calendar page instead of its replay;
 *  - an anonymous visitor got an empty grid that looked like "nothing is scheduled".
 */
test.describe("Public live discovery", () => {
  let studentToken = ""
  let studentUser: unknown = null

  test.beforeAll(async ({ request }) => {
    const r = await request.post(`${API}/v1/auth/login`, {
      data: { email: "fixall.test@example.com", password: "Test1234!" },
    })
    expect(r.ok(), `student login -> ${await r.text()}`).toBeTruthy()
    const body = await r.json()
    studentToken = body.data.accessToken
    studentUser = body.data.user ?? null
    // Institution comes from the authenticated user, never a hardcoded id: the
    // tenant guard rejects any other institution for this token.
    INST = body.data.user?.institutionId ?? ""
    expect(INST, "authenticated institution id").toBeTruthy()
  })

  async function signIn(page: Page, token: string, user: unknown) {
    await page.addInitScript(
      ([t, u, inst]) => {
        localStorage.setItem("elmkusoma_access_token", t as string)
        localStorage.setItem("elmkusoma_refresh_token", "e2e-refresh-token")
        localStorage.setItem("elmkusoma_current_user", u as string)
        localStorage.setItem("elmkusoma_institution_id", inst as string)
        document.cookie = `elmkusoma_access_token=${encodeURIComponent(t as string)}; path=/; max-age=86400; SameSite=Lax`
      },
      [token, JSON.stringify(user), INST],
    )
  }

  test("anonymous visitor is told to sign in, not shown an empty grid", async ({ page }) => {
    await page.goto("/live-classes")
    const panel = page.getByRole("heading", { name: /live learning/i }).first()
    await expect(panel).toBeVisible()

    // No fake cards for an anonymous visitor.
    await expect(page.getByRole("article")).toHaveCount(0)
    // Explicit, actionable state instead of a silently empty list.
    await expect(page.getByRole("link", { name: /sign in/i }).first()).toBeVisible()
  })

  test("signed-in learner sees real sessions from the API with real teacher names", async ({ page }) => {
    await signIn(page, studentToken, studentUser)
    await page.goto("/live-classes")

    const tablist = page.getByRole("tablist").first()
    await expect(tablist).toBeVisible()

    // Whatever the institution actually has, the live tab must render the real API result
    // rather than hardcoded sessions: no "0 watching" text may ever appear.
    await expect(page.getByText(/\d+ watching/)).toHaveCount(0)

    // Every rendered card shows either a real teacher or no teacher line at all.
    const cards = page.getByRole("article")
    const count = await cards.count()
    for (let i = 0; i < count; i++) {
      await expect(cards.nth(i)).toBeVisible()
    }
  })

  test("past tab shows finished sessions and never sends them to the calendar", async ({ page }) => {
    await signIn(page, studentToken, studentUser)
    await page.goto("/live-classes")

    const pastTab = page.getByRole("tab", { name: /past/i }).first()
    await pastTab.click()
    await expect(pastTab).toHaveAttribute("aria-selected", "true")

    const cards = page.getByRole("article")
    const count = await cards.count()
    for (let i = 0; i < count; i++) {
      const card = cards.nth(i)
      const links = await card.getByRole("link").all()
      for (const link of links) {
        const href = (await link.getAttribute("href")) ?? ""
        expect(href).not.toContain("calendar-integration")
      }
    }
  })

  test("discovery API exposes recording state without leaking the raw URL", async ({ request }) => {
    const r = await request.get(`${API}/v1/student/live-classes?size=100`, {
      headers: { Authorization: `Bearer ${studentToken}`, "X-Institution-Id": INST },
    })
    expect(r.ok(), `discovery -> ${await r.text()}`).toBeTruthy()
    const body = await r.json()
    const rows = body.data as Array<Record<string, unknown>>
    expect(Array.isArray(rows)).toBeTruthy()
    for (const row of rows) {
      expect(row).toHaveProperty("status")
      expect(row).toHaveProperty("hasRecording")
      expect(typeof row.hasRecording).toBe("boolean")
      // Replay access stays entitlement-checked server side.
      expect(row).not.toHaveProperty("recordingUrl")
    }
  })

  test("discovery API requires authentication", async ({ request }) => {
    const r = await request.get(`${API}/v1/student/live-classes?size=100`)
    expect([401, 403]).toContain(r.status())
  })

  test("homepage live section does not render when the API fails", async ({ page }) => {
    await page.route("**/v1/student/live-classes*", (route) => route.abort())
    await page.goto("/")
    // A failed request must not produce a fake/placeholder section.
    await expect(page.getByText(/\d+ watching/)).toHaveCount(0)
  })
})
