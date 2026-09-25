import { test, expect, type APIRequestContext, type Page } from "@playwright/test"

const API = "http://localhost:8080"
const INST = "a0000000-0000-0000-0000-000000000002"
const STUDENT_ID = "b0000000-0000-0000-0000-000000000020"
const TEACHER_ID = "c0000000-0000-0000-0000-000000000010"

test.describe.configure({ mode: "serial" })

test.use({
  permissions: ["camera", "microphone"],
  launchOptions: {
    args: [
      "--use-fake-ui-for-media-stream",
      "--use-fake-device-for-media-stream",
      "--autoplay-policy=no-user-gesture-required",
    ],
  },
})

let teacherToken = ""
let studentToken = ""
let endedClassId = ""
let liveClassId = ""
let scheduledClassId = ""

function localStamp(offsetMin: number): string {
  const d = new Date(Date.now() + offsetMin * 60000)
  const p = (n: number) => String(n).padStart(2, "0")
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

async function login(request: APIRequestContext, email: string, password: string): Promise<string> {
  const r = await request.post(`${API}/v1/auth/login`, { data: { email, password } })
  expect(r.ok(), `login ${email}`).toBeTruthy()
  const body = await r.json()
  return body.data.accessToken
}

async function createClass(request: APIRequestContext, title: string, offsetMin: number): Promise<string> {
  const r = await request.post(`${API}/v1/teachers/me/live-classes`, {
    headers: { Authorization: `Bearer ${teacherToken}`, "X-Institution-Id": INST },
    data: {
      title,
      description: "Playwright live player E2E",
      sessionType: "LECTURE",
      scheduledAt: localStamp(offsetMin),
      durationMinutes: 60,
      maxParticipants: 30,
      enableRecording: false,
    },
  })
  expect(r.ok(), `create ${title}: ${await r.text()}`).toBeTruthy()
  const body = await r.json()
  return body.data.id
}

async function teacherPost(path: string): Promise<void> {
  const r = await requestPostAsTeacher(path)
  expect(r.ok(), `${path} -> ${r.status()}`).toBeTruthy()
}

let apiRequest: APIRequestContext

async function requestPostAsTeacher(path: string) {
  return apiRequest.post(`${API}${path}`, {
    headers: { Authorization: `Bearer ${teacherToken}`, "X-Institution-Id": INST },
  })
}

async function seedAuth(page: Page, token: string, user: Record<string, unknown>) {
  await page.addInitScript(
    ([t, u, inst, refresh]) => {
      localStorage.setItem("elmkusoma_access_token", t as string)
      localStorage.setItem("elmkusoma_refresh_token", refresh as string)
      localStorage.setItem("elmkusoma_current_user", u as string)
      localStorage.setItem("elmkusoma_institution_id", inst as string)
      document.cookie = `elmkusoma_access_token=${encodeURIComponent(t as string)}; path=/; max-age=86400; SameSite=Lax`
      document.cookie = `elmkusoma_current_user=${encodeURIComponent(u as string)}; path=/; max-age=604800; SameSite=Lax`
    },
    [token, JSON.stringify(user), INST, "e2e-refresh-token"],
  )
}

const studentUser = {
  id: STUDENT_ID,
  name: "John Mushi",
  email: "student1@darms.edu.tz",
  role: "Student",
  institutionId: INST,
}

const teacherUser = {
  id: TEACHER_ID,
  name: "Amina Mwangi",
  email: "teacher1@darms.edu.tz",
  role: "Teacher",
  institutionId: INST,
}

const playerState = (page: Page) => page.locator("[data-live-player-state]")

test.beforeAll(async ({ playwright, request }) => {
  test.setTimeout(180_000)
  apiRequest = await playwright.request.newContext()
  teacherToken = await login(request, "teacher1@darms.edu.tz", "Test123!")
  studentToken = await login(request, "student1@darms.edu.tz", "Test123!")

  const list = await request.get(`${API}/v1/teachers/me/live-classes`, {
    headers: { Authorization: `Bearer ${teacherToken}`, "X-Institution-Id": INST },
  })
  if (list.ok()) {
    const body = await list.json()
    const items = body.data ?? body
    for (const c of Array.isArray(items) ? items : []) {
      if (c.status === "IN_PROGRESS" || c.status === "LIVE") {
        await apiRequest
          .post(`${API}/v1/teachers/me/live-classes/${c.id}/end`, {
            headers: { Authorization: `Bearer ${teacherToken}`, "X-Institution-Id": INST },
          })
          .catch(() => {})
      } else if (c.status === "SCHEDULED" || c.status === "STARTING") {
        await apiRequest
          .delete(`${API}/v1/teachers/me/live-classes/${c.id}`, {
            headers: { Authorization: `Bearer ${teacherToken}`, "X-Institution-Id": INST },
          })
          .catch(() => {})
      }
    }
  }

  endedClassId = await createClass(request, `E2E Ended ${Date.now()}`, 1)
  await teacherPost(`/v1/teachers/me/live-classes/${endedClassId}/start`)
  await teacherPost(`/v1/teachers/me/live-classes/${endedClassId}/end`)

  liveClassId = await createClass(request, `E2E Live ${Date.now()}`, 5)
  await teacherPost(`/v1/teachers/me/live-classes/${liveClassId}/start`)

  scheduledClassId = await createClass(request, `E2E Scheduled ${Date.now()}`, 25)
})

test.afterAll(async () => {
  if (liveClassId) {
    await apiRequest.post(`${API}/v1/teachers/me/live-classes/${liveClassId}/end`, {
      headers: { Authorization: `Bearer ${teacherToken}`, "X-Institution-Id": INST },
    }).catch(() => {})
  }
  await apiRequest?.dispose()
})

test("scheduled class shows SCHEDULED state and no LIVE badge", async ({ page }) => {
  await seedAuth(page, studentToken, studentUser)
  await page.goto(`/live-classes/${scheduledClassId}`)
  await expect(playerState(page)).toHaveAttribute("data-live-player-state", "scheduled", { timeout: 45000 })
  await expect(playerState(page).getByText("LIVE", { exact: true })).toHaveCount(0)
})

test("ended class shows ENDED state, recording copy, and no LIVE badge", async ({ page }) => {
  await seedAuth(page, studentToken, studentUser)
  await page.goto(`/live-classes/${endedClassId}`)
  await expect(playerState(page)).toHaveAttribute("data-live-player-state", "ended", { timeout: 45000 })
  await expect(playerState(page).getByText("Live session ended")).toBeVisible()
  await expect(playerState(page).getByText("LIVE", { exact: true })).toHaveCount(0)
})

test("in-progress class connects to LiveKit and shows WAITING state with LIVE badge at 16:9", async ({ page }) => {
  await seedAuth(page, studentToken, studentUser)
  await page.goto(`/live-classes/${liveClassId}`)

  await expect(playerState(page)).toHaveAttribute("data-live-player-state", "waiting", { timeout: 60000 })
  await expect(playerState(page).getByText("LIVE", { exact: true })).toBeVisible()
  await expect(playerState(page).getByText("Waiting for the teacher's live screen...").first()).toBeVisible()

  const box = await playerState(page).boundingBox()
  expect(box).not.toBeNull()
  const ratio = box!.width / box!.height
  expect(ratio).toBeGreaterThan(1.7)
  expect(ratio).toBeLessThan(1.85)

  await expect(page.getByPlaceholder("Type a message...")).toBeVisible()
  await expect(page.getByRole("button", { name: "Raise hand" })).toBeVisible()
  await expect(page.getByRole("button", { name: "Attach material" })).toBeVisible()
})

test("teacher publishes camera -> learner sees LIVE state with remote video", async ({ browser }) => {
  const teacherCtx = await browser.newContext()
  const learnerCtx = await browser.newContext()
  const teacherPage = await teacherCtx.newPage()
  const learnerPage = await learnerCtx.newPage()

  try {
    await seedAuth(teacherPage, teacherToken, teacherUser)
    await seedAuth(learnerPage, studentToken, studentUser)

    await teacherPage.goto(`/live-classes/${liveClassId}`)
    await expect(playerState(teacherPage)).toHaveAttribute(
      "data-live-player-state",
      /waiting|live/,
      { timeout: 60000 },
    )
    await expect(teacherPage.getByText("Waiting for the teacher's live screen...")).toHaveCount(0)
    await expect(teacherPage.getByText("Start your live screen")).toBeVisible()

    const camButton = teacherPage.getByRole("button", { name: "Turn on camera" })
    await expect(camButton).toBeVisible({ timeout: 30000 })
    await camButton.click()

    await learnerPage.goto(`/live-classes/${liveClassId}`)
    await expect(playerState(learnerPage)).toHaveAttribute(
      "data-live-player-state",
      "live",
      { timeout: 60000 },
    )
    await expect(learnerPage.locator('video[aria-label="Live video"]')).toBeVisible({ timeout: 30000 })
    await expect(playerState(learnerPage).getByText("LIVE", { exact: true })).toBeVisible()
  } finally {
    await teacherCtx.close()
    await learnerCtx.close()
  }
})

test("ending the session flips player to ENDED and removes LIVE badge", async ({ page }) => {
  await seedAuth(page, studentToken, studentUser)
  await page.goto(`/live-classes/${liveClassId}`)
  await expect(playerState(page)).toHaveAttribute(
    "data-live-player-state",
    /waiting|live/,
    { timeout: 60000 },
  )

  const endResp = await apiRequest.post(`${API}/v1/teachers/me/live-classes/${liveClassId}/end`, {
    headers: { Authorization: `Bearer ${teacherToken}`, "X-Institution-Id": INST },
  })
  expect(endResp.ok()).toBeTruthy()

  await expect(playerState(page)).toHaveAttribute("data-live-player-state", "ended", { timeout: 45000 })
  await expect(playerState(page).getByText("LIVE", { exact: true })).toHaveCount(0)
})
