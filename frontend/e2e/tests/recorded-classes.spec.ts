import { test, expect } from "@playwright/test"

// The API host is overridable (the E2E gate runs this suite against an instance other than
// the default 8080); the default is unchanged for existing callers.
const API = process.env.E2E_API_URL || "http://localhost:8080"
const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || "http://localhost:3000"

/**
 * Recorded Classes — discoverability across all seven learner categories.
 *
 * The authorisation half of "a learner can watch a published recording" is covered by the
 * backend suites (RecordedClassesAudienceEntitlementTest, RecordingPlaybackAuthorizationTest,
 * MediaLibraryRecordingAuthorizationTest, ReplayEntitlementTest): they assert that the list,
 * the detail and the authorised byte route accept audience membership as well as attendance,
 * and default-deny otherwise. What none of them can cover is whether a learner can *find* the
 * feature at all — the sidebar branch is chosen purely from the client-side user shape
 * (getStudentNavSections() / getStudentNavFlat() in dashboard-sidebar.tsx), so every one of the
 * seven categories needs its own assertion.
 *
 * The category is driven by the signed-in user's learningLevel / role rather than by seven
 * provisioned accounts: the session token stays real, only the client-side identity changes,
 * which is exactly the input the sidebar uses to pick its navigation.
 */

const RECORDED_CLASSES_HREF = "/dashboard/learner/replays"
const MEDIA_LIBRARY_HREF = "/dashboard/learner/media-library"

/** English and Swahili labels, so the assertion holds whichever locale the browser resolves to. */
const RECORDED_CLASSES_LABEL = /Recorded Classes|Madarasa Yaliyorekodiwa/

type Category = {
  name: string
  role: string
  /** null for the role-driven "Other Learner" branch, which ignores learningLevel. */
  learningLevel: string | null
  /**
   * Collapsed group header the entry sits under, for the navs that group their items.
   * Flat navs (nursery / primary / secondary) render every item directly.
   */
  group: string | null
}

const CATEGORIES: Category[] = [
  { name: "Nursery", role: "Student", learningLevel: "NURSERY", group: null },
  { name: "Primary", role: "Student", learningLevel: "PRIMARY", group: null },
  { name: "Secondary", role: "Student", learningLevel: "SECONDARY", group: null },
  { name: "VETA", role: "Student", learningLevel: "VETA", group: "LIVE & MEDIA" },
  { name: "College", role: "Student", learningLevel: "COLLEGE", group: "LIVE & MEDIA" },
  { name: "University", role: "Student", learningLevel: "UNIVERSITY", group: "Live Campus" },
  { name: "Other Learners", role: "Other Learner", learningLevel: null, group: "DISCOVER" },
]

test.describe("Recorded Classes discoverability", () => {
  let token = ""
  let institutionId = ""
  let baseUser: Record<string, unknown> = {}

  test.beforeAll(async ({ request }) => {
    const r = await request.post(`${API}/v1/auth/login`, {
      data: { email: "student1@darms.edu.tz", password: "password" },
    })
    expect(r.ok(), `student login -> ${await r.text()}`).toBeTruthy()
    const body = await r.json()
    token = body.data.accessToken
    baseUser = body.data.user ?? {}
    institutionId = body.data.user?.institutionId ?? ""
    expect(token, "authenticated access token").toBeTruthy()
    expect(institutionId, "authenticated institution id").toBeTruthy()
  })

  /**
   * Signs the page in as a real student whose client-side identity carries the category under
   * test. The user object mirrors what lib/auth.tsx mapUserInfo() writes at login, so the
   * sidebar sees the same shape it would after a real sign-in at that learning level.
   */
  async function seedAsCategory(
    page: import("@playwright/test").Page,
    category: Category,
  ) {
    const u = baseUser
    const user = {
      id: u.id,
      name: u.fullName || [u.firstName, u.lastName].filter(Boolean).join(" "),
      email: u.email,
      role: category.role,
      firstName: u.firstName,
      lastName: u.lastName,
      phone: u.phone,
      institutionId,
      learningLevel: category.learningLevel,
    }
    // proxy.ts (Next 16's middleware, renamed from middleware.ts) gates /dashboard/* on the
    // auth *cookie*: it runs on the server, so localStorage is invisible to it and a
    // document.cookie written in an init script is still too late for the first request.
    // The cookies therefore go into the browser context's jar before the page ever loads.
    await page.context().addCookies([
      { name: "elmkusoma_access_token", value: token, url: BASE_URL },
      { name: "elmkusoma_current_user", value: encodeURIComponent(JSON.stringify(user)), url: BASE_URL },
    ])
    // The client half (lib/auth.tsx) reads localStorage, so seed both.
    await page.addInitScript(
      ([t, uJson, inst]) => {
        localStorage.setItem("elmkusoma_access_token", t as string)
        localStorage.setItem("elmkusoma_refresh_token", "e2e-refresh-token")
        localStorage.setItem("elmkusoma_current_user", uJson as string)
        localStorage.setItem("elmkusoma_institution_id", inst as string)
      },
      [token, JSON.stringify(user), institutionId],
    )
  }

  for (const category of CATEGORIES) {
    test(`${category.name} learner finds Recorded Classes in the sidebar`, async ({ page }) => {
      await seedAsCategory(page, category)
      await page.goto(RECORDED_CLASSES_HREF)

      // The shared dashboard sidebar, not the topbar's own landmarks.
      const sidebar = page.locator("aside")
      await expect(sidebar).toBeVisible()

      const entry = sidebar.getByRole("link", { name: RECORDED_CLASSES_LABEL }).first()
      if (category.group) {
        // Grouped navs hide their items until the section is opened — but the section holding
        // the current route is auto-expanded, so only click when the entry is still hidden.
        if (!(await entry.isVisible().catch(() => false))) {
          await sidebar.getByRole("button", { name: category.group }).click()
        }
      }
      await expect(entry).toBeVisible()
      await expect(entry).toHaveAttribute("href", RECORDED_CLASSES_HREF)

      // The learner layout renders #main-content only when it admits the learner; the denied
      // branch renders a bare alert instead. Asserting the outer <main> from the dashboard
      // layout would pass either way and hide a denial.
      await expect(page).toHaveURL(new RegExp(RECORDED_CLASSES_HREF + "$"))
      await expect(page.locator("#main-content")).toBeVisible()
      await expect(page.getByText(/Access denied|Madarasa imezuiwa/i)).toHaveCount(0)
    })
  }

  test("Recorded Classes sits next to the live entry it complements", async ({ page }) => {
    await seedAsCategory(page, { name: "Secondary", role: "Student", learningLevel: "SECONDARY", group: null })
    await page.goto(RECORDED_CLASSES_HREF)

    const sidebar = page.locator("aside")
    // Wait for the nav to render before reading it; a flat nav shows both entries directly.
    await expect(sidebar.getByRole("link", { name: RECORDED_CLASSES_LABEL })).toBeVisible()

    const hrefs = await sidebar.locator("a").evaluateAll((as) => as.map((a) => a.getAttribute("href")))
    const recordedAt = hrefs.indexOf(RECORDED_CLASSES_HREF)
    expect(recordedAt, "Recorded Classes present in the sidebar").toBeGreaterThanOrEqual(0)
    // Live comes first, the recording of it second.
    const liveAt = hrefs.findIndex((h) => h === "/dashboard/secondary/live")
    expect(liveAt, "live entry present").toBeGreaterThanOrEqual(0)
    expect(recordedAt).toBe(liveAt + 1)
  })

  test("both recorded-classes surfaces render for a signed-in learner", async ({ page }) => {
    await seedAsCategory(page, { name: "College", role: "Student", learningLevel: "COLLEGE", group: "LIVE & MEDIA" })

    await page.goto(RECORDED_CLASSES_HREF)
    await expect(page.locator("#main-content")).toBeVisible()
    await expect(page.getByText(/Access denied|Madarasa imezuiwa/i)).toHaveCount(0)
    // Never silently redirected away from the learner's own recorded classes.
    await expect(page).toHaveURL(new RegExp(RECORDED_CLASSES_HREF + "$"))
    const sidebar = page.locator("aside")
    const entry = sidebar.getByRole("link", { name: RECORDED_CLASSES_LABEL }).first()
    if (!(await entry.isVisible().catch(() => false))) {
      await sidebar.getByRole("button", { name: "LIVE & MEDIA" }).click()
    }
    await expect(entry).toBeVisible()

    await page.goto(MEDIA_LIBRARY_HREF)
    await expect(page).toHaveURL(new RegExp(MEDIA_LIBRARY_HREF + "$"))
    await expect(page.locator("#main-content")).toBeVisible()
    await expect(page.getByText(/Access denied|Madarasa imezuiwa/i)).toHaveCount(0)
  })

  test("signed-out visitors are sent to login, not shown an empty library", async ({ page }) => {
    await page.goto(RECORDED_CLASSES_HREF)
    await expect(page).toHaveURL(/login/)
  })

  test("the playback route is server-authorised and never hands out a storage URL", async ({
    request,
  }) => {
    // A non-existent recording must be refused by the authorising route itself: no redirect to a
    // private bucket, no signed object URL in the body. Playback is served from the authorised
    // path only (see RecordingPlaybackAuthorizationTest for the entitled case).
    const res = await request.get(`${API}/v1/replays/00000000-0000-0000-0000-0000000000ff/playback`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect([401, 403, 404, 502], `playback status was ${res.status()}`).toContain(res.status())
    const body = await res.text()
    expect(body).not.toMatch(/backblazeb2|\.b2api\.|amazonaws\.com/)
  })

  test("the entitlement-filtered learner replay feed responds for the signed-in learner", async ({
    request,
  }) => {
    const res = await request.get(`${API}/v1/learner/replays`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(res.status(), `learner replays -> ${res.status()}`).toBe(200)
    const body = await res.json()
    expect(Array.isArray(body.data), "replay feed is a list").toBe(true)
  })
})
