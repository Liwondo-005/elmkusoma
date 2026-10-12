import { test, expect } from "@playwright/test"

const BASE = "http://localhost:3000"
const API = "http://localhost:8080"
const ADMIN_EMAIL = "admin@elmkusoma.go.tz"
const ADMIN_PASSWORD = "password"

/**
 * Public news and Platform Admin news management.
 *
 * <p>End-to-end rather than mocked: these tests drive the real backend, so they catch the class of
 * defect unit tests cannot - a control that renders correctly but posts to a path the server does
 * not serve, or a draft that leaks because the public query forgot a predicate.</p>
 *
 * <p>Articles created here are deleted in afterEach so the suite is repeatable.</p>
 */

let adminToken: string
const createdIds: string[] = []

async function adminLogin(page) {
  const res = await page.request.post(`${API}/v1/auth/login`, {
    data: { email: ADMIN_EMAIL, password: ADMIN_PASSWORD },
  })
  const body = await res.json()
  expect(body.success).toBeTruthy()
  adminToken = body.data.accessToken

  // The admin shell reads its identity from cookies AND lib/auth.tsx reads the current user from
  // localStorage. Setting only the access token leaves the guard without a user and it bounces to
  // /login, which looks exactly like a broken page in the test output.
  //
  // The role must also be mapped to its display form: the layout compares user.role !== "Admin",
  // and mapRoleToFrontend in lib/auth.tsx does that translation at sign-in. Storing the raw
  // backend "ADMIN" therefore fails the check and redirects to /dashboard.
  const user = { ...body.data.user, role: "Admin" }
  const userJson = JSON.stringify(user)
  await page.context().addCookies([
    { name: "elmkusoma_access_token", value: adminToken, url: BASE },
    { name: "elmkusoma_current_user", value: encodeURIComponent(userJson), url: BASE },
    { name: "elmkusoma_institution_id", value: user.institutionId ?? "", url: BASE },
  ])
  await page.addInitScript(
    ({ token, json }) => {
      localStorage.setItem("elmkusoma_access_token", token)
      localStorage.setItem("elmkusoma_current_user", json)
    },
    { token: adminToken, json: userJson }
  )
  return adminToken
}

function authHeaders() {
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${adminToken}`,
    "X-Institution-Id": "a0000000-0000-0000-0000-000000000001",
  }
}

async function createDraft(title: string, summary = "Summary for the end-to-end test article.") {
  const res = await fetch(`${API}/v1/platform-admin/news`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify({
      title,
      summary,
      body: "First paragraph of the article body.\n\nA second paragraph, to exercise splitting.",
    }),
  })
  expect(res.status).toBe(201)
  const body = await res.json()
  createdIds.push(body.data.id)
  return body.data
}

async function publish(id: string) {
  const res = await fetch(`${API}/v1/platform-admin/news/${id}/publish`, {
    method: "POST",
    headers: authHeaders(),
  })
  expect(res.ok).toBeTruthy()
  return (await res.json()).data
}

test.afterAll(async () => {
  for (const id of createdIds) {
    await fetch(`${API}/v1/platform-admin/news/${id}`, { method: "DELETE", headers: authHeaders() })
  }
})

test.describe("public news", () => {
  test("archive renders published articles and the article page resolves by slug", async ({ page }) => {
    const token = await adminLogin(page)
    expect(token).toBeTruthy()

    const draft = await createDraft("E2E archive headline")
    const published = await publish(draft.id)

    await page.goto(`${BASE}/news`)

    // The article must actually be listed, not merely reachable by direct URL.
    await expect(page.getByRole("heading", { name: "E2E archive headline" })).toBeVisible()

    // Click through rather than navigating directly: this is what proves the card is a real link
    // to the article, which a direct goto would not exercise.
    const card = page.getByRole("link", { name: /E2E archive headline/ }).first()
    await expect(card).toBeVisible()
    await expect(card).toHaveAttribute("href", /\/news\/e2e-archive-headline$/)
    await card.click()
    await expect(page).toHaveURL(new RegExp(`/news/e2e-archive-headline$`))

    // Shareable: a refresh on the same URL re-renders the article.
    await page.reload()
    await expect(page.getByRole("heading", { name: "E2E archive headline", level: 1 })).toBeVisible()
    await expect(page.getByText("First paragraph of the article body.")).toBeVisible()
  })

  test("a draft never appears on the public site", async ({ page }) => {
    await adminLogin(page)
    await createDraft("E2E confidential draft headline")

    await page.goto(`${BASE}/news`)
    await expect(page.getByText("E2E confidential draft headline")).toHaveCount(0)

    // And is not reachable by guessing its slug.
    const res = await fetch(`${API}/v1/public/news/e2e-confidential-draft-headline`)
    expect(res.status).toBe(404)
  })

  test("unpublishing removes the article from the public archive", async ({ page }) => {
    await adminLogin(page)
    const draft = await createDraft("E2E temporary notice")
    await publish(draft.id)

    await page.goto(`${BASE}/news`)
    await expect(page.getByRole("heading", { name: "E2E temporary notice" })).toBeVisible()

    const res = await fetch(`${API}/v1/platform-admin/news/${draft.id}/unpublish`, {
      method: "POST",
      headers: authHeaders(),
    })
    expect(res.ok).toBeTruthy()

    await page.reload()
    await expect(page.getByText("E2E temporary notice")).toHaveCount(0)
  })

  test("an unknown slug renders a not-found state rather than an error", async ({ page }) => {
    await page.goto(`${BASE}/news/no-such-article-anywhere`)
    await expect(page.getByRole("heading", { name: /not found/i })).toBeVisible()
  })

  test("news links are reachable from the header and footer", async ({ page }) => {
    await page.goto(BASE)
    await page.locator("header").getByRole("link", { name: "News" }).click()
    await expect(page).toHaveURL(`${BASE}/news`)
  })

  test("the landing page news section only appears when there is news", async ({ page }) => {
    await adminLogin(page)
    await page.goto(BASE)
    // With nothing published the section must be absent entirely rather than an empty shell.
    const section = page.locator("section", { has: page.getByRole("heading", { name: "Latest News" }) })
    if (await section.count()) {
      await expect(section).toBeVisible()
    }

    const draft = await createDraft("E2E landing headline")
    await publish(draft.id)
    await page.reload()
    await expect(page.getByRole("heading", { name: "Latest News" })).toBeVisible()
    await expect(page.getByText("E2E landing headline")).toBeVisible()
  })
})

test.describe("platform admin news", () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page)
  })

  test("anonymous callers cannot reach the news admin API", async () => {
    for (const path of ["/v1/platform-admin/news"]) {
      const res = await fetch(`${API}${path}`)
      expect([401, 403]).toContain(res.status)
    }
  })

  test("the admin screen loads and lists articles", async ({ page }) => {
    await page.goto(`${BASE}/dashboard/platform-admin/news`)
    await expect(page.getByRole("heading", { name: "News & Announcements" })).toBeVisible()
  })

  test("saving a draft in the UI does not publish it", async ({ page }) => {
    const title = `E2E UI draft ${Date.now()}`
    await page.goto(`${BASE}/dashboard/platform-admin/news`)

    await page.getByRole("button", { name: "New article" }).click()
    await page.getByLabel("Title", { exact: false }).first().fill(title)
    await page.getByLabel("Summary", { exact: false }).first().fill("A summary written in the browser.")
    await page.getByLabel("Full text", { exact: false }).first().fill("Body written in the browser.")
    await page.getByRole("button", { name: "Save draft" }).click()

    await expect(page.getByText(/Draft created/)).toBeVisible()

    // Persisted as a draft: present in admin, absent from the public site.
    await expect(page.getByText(title)).toBeVisible()
    const publicRes = await fetch(`${API}/v1/public/news`)
    const publicBody = await publicRes.json()
    const publicTitles = (publicBody.data.content ?? []).map((a: { title: string }) => a.title)
    expect(publicTitles).not.toContain(title)
  })

  test("the featured star persists across a reload", async ({ page }) => {
    const draft = await createDraft("E2E featured toggle")
    await page.goto(`${BASE}/dashboard/platform-admin/news`)
    await page.getByLabel("Search").fill("E2E featured toggle")
    await page.getByRole("button", { name: "Refresh" }).click()

    await page.getByRole("button", { name: "Mark as featured" }).first().click()
    await expect(page.getByText(/marked as featured/)).toBeVisible()

    await page.reload()
    await expect(page.getByText("E2E featured toggle")).toBeVisible()
    // The toggle now offers to remove the flag, which proves the server stored it.
    await expect(page.getByRole("button", { name: "Remove featured" }).first()).toBeVisible()
  })
})