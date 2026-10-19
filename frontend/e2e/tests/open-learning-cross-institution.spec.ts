import { test, expect, type APIRequestContext, type Page } from "@playwright/test"
import { execFileSync } from "node:child_process"
import { existsSync } from "node:fs"

/**
 * Independent learning, cross-institution open sessions and the Platform Admin teacher
 * provisioning UI — real browser flows against a running stack.
 *
 * Covers the browser E2E scenarios end to end:
 *  1/2/4  the published open session reaches learners of the host school, of ANOTHER
 *         school and of NO school, through the real discovery surface;
 *  3      an independent teacher signs in with the real login form and manages her own
 *         published session with no school attached (server forces it to the open
 *         audience — never someone else's scope);
 *  5      the school learner keeps their own school placement;
 *  6      the school-private session stays invisible to outsiders, in listing AND on a
 *         direct detail fetch;
 *  7      a teacher cannot act on a session outside their own scope (403 pinned) and
 *         the UI never shows it to them;
 *  8      the Media Library is consistent per audience — listing, detail and download-url
 *         agree server side; the open recording crosses institutions and reaches
 *         institution-less learners; school-private material never does; a reader with
 *         no tenant gets a clean page instead of an error surface;
 *  11     a Platform Admin provisions BOTH supported teacher types through the existing
 *         user form (independent = school-less, affiliated = school still required);
 *  12     English and Swahili both render.
 *
 * Data discipline: every fixture created here is run-suffixed; deletion is attempted only
 * for records this run created and refusals are logged rather than worked around.
 * Pre-existing dev accounts (documented seed admin/teacher/student "@…" / "password")
 * are used read-only for the roles a test does not need to provision.
 *
 * E2E_API_URL selects the backend under test (repo convention 8080; the final-tree run
 * points it at the independently started instance of this working tree).
 */
const API = process.env.E2E_API_URL || "http://localhost:8080"
// Where the browser under test lives (cookies must be scoped to it).
const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || "http://localhost:3000"
const DMS = "a0000000-0000-0000-0000-000000000002" // Dar es Salaam Model School (host school)
const MPA = "a0000000-0000-0000-0000-000000000004" // Mwanza Primary Academy (another school)
const PSQL = "C:\\Program Files\\PostgreSQL\\18\\bin\\psql.exe"

const ADMIN = { email: "admin@elmkusoma.go.tz", password: "password" }
const HOST_TEACHER = { email: "teacher1@darms.edu.tz", password: "password" }
const SCHOOL_LEARNER = { email: "student1@darms.edu.tz", password: "password" }

const SFX = Date.now().toString(36).toUpperCase()
const otherSchoolEmail = `e2e-mpa-learner-${SFX}@test.com`
const independentEmail = `e2e-independent-${SFX}@test.com`
const independentTeacherEmail = `e2e-indep-teacher-${SFX}@test.com`
const affiliatedEmail = `e2e-affiliated-${SFX}@test.com`
const TEST_PASSWORD = "Test123!"

const openTitle = `E2E Open Session ${SFX}`
const independentSessionTitle = `E2E Independent Session ${SFX}`
const privateTitle = `E2E Private Class ${SFX}`
const openRecordingTitle = `E2E Open Recording ${SFX}`
const privateDocTitle = `E2E Private Document ${SFX}`

test.describe.configure({ mode: "serial" })

let adminToken = ""
let adminUser: Record<string, unknown> = {}
let hostToken = ""
let hostUser: Record<string, unknown> = {}
let studentToken = ""
let studentUser: Record<string, unknown> = {}
let otherSchoolToken = ""
let otherSchoolUser: Record<string, unknown> = {}
let independentToken = ""
let independentUser: Record<string, unknown> = {}
let independentTeacherToken = ""
let independentTeacherUser: Record<string, unknown> = {}

let openClassId = ""
let privateClassId = ""
let independentClassId = ""
let privateDocId = ""
let openRecordingId = ""

function localStamp(offsetMin: number): string {
  const d = new Date(Date.now() + offsetMin * 60000)
  const p = (n: number) => String(n).padStart(2, "0")
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const ROLE_TO_DISPLAY: Record<string, string> = {
  STUDENT: "Student",
  TEACHER: "Teacher",
  PARENT: "Parent",
  OTHER_LEARNER: "Other Learner",
  LEARNER: "Learner",
  ADMIN: "Admin",
  INSTITUTION_ADMIN: "Institution Admin",
  NATIONAL_ADMIN: "National Admin",
  REGIONAL_ADMIN: "Regional Admin",
  DISTRICT_ADMIN: "District Admin",
  PROVIDER_ADMIN: "Provider Admin",
  PROVIDER_STAFF: "Provider Staff",
}

/**
 * A real login persists the user EXACTLY as frontend/lib/auth.tsx builds it:
 * `mapUserInfo` adds `name` and `mapRoleToFrontend` converts the backend enum to its
 * display form before setCurrentUser. Guards compare the DISPLAY form
 * (`user.role !== "Admin"` in the platform-admin layout), so seeded state must match
 * what the app itself would have written — raw API enums would bounce to /login.
 */
function toAuthUser(raw: Record<string, unknown>): Record<string, unknown> {
  const role = String(raw.role ?? "")
  const name =
    String(raw.fullName ?? "") ||
    [String(raw.firstName ?? ""), String(raw.lastName ?? "")].filter(Boolean).join(" ")
  return { ...raw, name, role: ROLE_TO_DISPLAY[role] || role }
}

async function login(
  request: APIRequestContext,
  email: string,
  password: string
): Promise<{ token: string; user: Record<string, unknown> }> {
  const r = await request.post(`${API}/v1/auth/login`, { data: { email, password } })
  const text = await r.text() // body read exactly once
  expect(r.ok(), `login ${email} -> ${r.status()}: ${text}`).toBeTruthy()
  const body = JSON.parse(text)
  return { token: body.data.accessToken, user: toAuthUser(body.data.user) }
}

/**
 * Real authenticated state: the exact user object the server returned at login, persisted
 * exactly the way lib/auth.tsx would persist it (mapped role + name).
 *
 * proxy.ts (Next 16's middleware, renamed from middleware.ts) gates /dashboard/* on the
 * auth *cookie*: it runs on the server, so localStorage is invisible to it and a
 * document.cookie written in an init script is still too late for the first request.
 * The cookies therefore go into the browser context's jar before the page ever loads.
 */
async function seedAuth(page: Page, token: string, user: Record<string, unknown>) {
  await page.context().addCookies([
    { name: "elmkusoma_access_token", value: token, url: BASE_URL },
    { name: "elmkusoma_current_user", value: encodeURIComponent(JSON.stringify(user)), url: BASE_URL },
  ])
  await page.addInitScript(
    ([t, u]) => {
      const parsed = JSON.parse(u as string)
      localStorage.setItem("elmkusoma_access_token", t as string)
      localStorage.setItem("elmkusoma_refresh_token", "e2e-refresh-token")
      localStorage.setItem("elmkusoma_current_user", u as string)
      // No school on the account -> no institution id -> no tenant header is sent.
      localStorage.setItem("elmkusoma_institution_id", parsed?.institutionId ?? "")
      document.cookie = `elmkusoma_access_token=${encodeURIComponent(t as string)}; path=/; max-age=86400; SameSite=Lax`
      document.cookie = `elmkusoma_current_user=${encodeURIComponent(u as string)}; path=/; max-age=604800; SameSite=Lax`
    },
    [token, JSON.stringify(user)]
  )
}

async function asAdmin(path: string, init?: RequestInit) {
  return fetch(`${API}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${adminToken}`,
      ...((init?.headers as Record<string, string>) || {}),
    },
  })
}

async function asHost(path: string, init?: RequestInit) {
  return fetch(`${API}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${hostToken}`,
      "X-Institution-Id": DMS,
      ...((init?.headers as Record<string, string>) || {}),
    },
  })
}

function psql(sql: string): string {
  if (!existsSync(PSQL)) {
    throw new Error(
      `psql not found at ${PSQL}: the open-recording fixture (LIVE_CLASS is a reserved origin only the recording hand-off may write) cannot be created without it`
    )
  }
  const out = execFileSync(
    PSQL,
    ["-h", "localhost", "-U", "postgres", "-d", "elmkusoma", "-w", "-t", "-A", "-c", sql],
    { env: { ...process.env, PGPASSWORD: process.env.DB_PASSWORD || "mwesiga" }, encoding: "utf8" }
  )
  // psql may append its command tag ("INSERT 0 1") after the result row: return the
  // first line that is actually a uuid (fallback: first non-empty line).
  const lines = out
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean)
  return lines.find((l) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(l)) ?? lines[0] ?? ""
}

test.beforeAll(async ({ playwright, request }) => {
  test.setTimeout(300_000)
  const apiRequest = await playwright.request.newContext()

  const admin = await login(apiRequest, ADMIN.email, ADMIN.password)
  adminToken = admin.token
  adminUser = admin.user
  const host = await login(apiRequest, HOST_TEACHER.email, HOST_TEACHER.password)
  hostToken = host.token
  hostUser = host.user
  const student = await login(apiRequest, SCHOOL_LEARNER.email, SCHOOL_LEARNER.password)
  studentToken = student.token
  studentUser = student.user

  // Another school's learner: provisioned through the existing Platform Admin API so the
  // account is a real member of a real, different institution.
  const created = await asAdmin("/v1/platform-admin/users", {
    method: "POST",
    body: JSON.stringify({
      firstName: "MPA",
      lastName: `Learner${SFX.slice(-4)}`,
      email: otherSchoolEmail,
      password: TEST_PASSWORD,
      role: "STUDENT",
      institutionId: MPA,
    }),
  })
  const createdText = await created.text()
  expect(created.status, `other-school learner create: ${createdText}`).toBe(201)
  const other = await login(apiRequest, otherSchoolEmail, TEST_PASSWORD)
  otherSchoolToken = other.token
  otherSchoolUser = other.user

  // Institution-less learner: self-registration, as the product intends.
  const reg = await request.post(`${API}/v1/auth/register`, {
    data: {
      firstName: "E2E",
      lastName: `Independent${SFX.slice(-4)}`,
      email: independentEmail,
      password: TEST_PASSWORD,
      role: "OTHER_LEARNER",
    },
  })
  const regText = await reg.text()
  expect(reg.status(), `independent register: ${regText}`).toBe(201)
  const indep = await login(apiRequest, independentEmail, TEST_PASSWORD)
  independentToken = indep.token
  independentUser = indep.user

  // Host-school teacher creates the OPEN session (explicitly PUBLIC, no class scope) and
  // a SCHOOL-PRIVATE session (institution audience) through the real teacher API.
  const openResp = await asHost("/v1/teachers/me/live-classes", {
    method: "POST",
    body: JSON.stringify({
      title: openTitle,
      description: "E2E cross-institution open learning session",
      sessionType: "LECTURE",
      scheduledAt: localStamp(6),
      durationMinutes: 60,
      maxParticipants: 30,
      enableRecording: false,
      accessLevel: "PUBLIC",
    }),
  })
  const openText = await openResp.text()
  expect(openResp.status, `open create: ${openText}`).toBe(201)
  const openBody = JSON.parse(openText).data
  openClassId = openBody.id
  expect(openBody.publicationStatus).toBe("DRAFT")

  const privateResp = await asHost("/v1/teachers/me/live-classes", {
    method: "POST",
    body: JSON.stringify({
      title: privateTitle,
      description: "E2E school-private session",
      sessionType: "LECTURE",
      scheduledAt: localStamp(7),
      durationMinutes: 45,
      maxParticipants: 30,
      enableRecording: false,
      accessLevel: "INSTITUTION",
    }),
  })
  const privateText = await privateResp.text()
  expect(privateResp.status, `private create: ${privateText}`).toBe(201)
  privateClassId = JSON.parse(privateText).data.id

  // Governance for both sessions: teacher submit -> platform-admin approve.
  for (const classId of [openClassId, privateClassId]) {
    const submit = await fetch(`${API}/v1/teachers/me/live-classes/${classId}/submit-for-approval`, {
      method: "POST",
      headers: { Authorization: `Bearer ${hostToken}`, "X-Institution-Id": DMS },
    })
    const submitText = await submit.text()
    expect(submit.ok, `submit ${classId}: ${submit.status} ${submitText}`).toBeTruthy()

    const approve = await asAdmin(`/v1/platform-admin/live-classes/governance/${classId}/approve`, {
      method: "POST",
      body: JSON.stringify({ note: "E2E open learning run" }),
    })
    const approveText = await approve.text()
    expect(approve.ok, `approve ${classId}: ${approve.status} ${approveText}`).toBeTruthy()
  }

  // Contract check: publish is the PUBLIC-listing gate and must refuse institution scope
  // (documented design: an approved INSTITUTION class never goes onto the public site;
  // its own school reaches it through membership scoping instead).
  const refusedPublish = await asAdmin(
    `/v1/platform-admin/live-classes/governance/${privateClassId}/publish`,
    { method: "POST" }
  )
  const refusedText = await refusedPublish.text()
  console.log(`[evidence] publish of institution-scoped session answered ${refusedPublish.status}: ${refusedText}`)
  expect(refusedPublish.status, "institution scope must be refused by the public publish gate").toBe(409)

  // Only the PUBLIC session is published — that is what makes it universally discoverable.
  const publish = await asAdmin(`/v1/platform-admin/live-classes/governance/${openClassId}/publish`, {
    method: "POST",
  })
  const publishText = await publish.text()
  expect(publish.ok, `publish open session: ${publish.status} ${publishText}`).toBeTruthy()

  // School-private media through the real create endpoint (server-side audience rules).
  const docResp = await asHost("/v1/media", {
    method: "POST",
    body: JSON.stringify({
      title: privateDocTitle,
      description: "E2E school-private document",
      mediaType: "DOCUMENT",
      fileUrl: `https://cdn.test/e2e-private-${SFX}.pdf`,
      status: "READY",
    }),
  })
  const docText = await docResp.text()
  expect(docResp.status, `private doc create: ${docText}`).toBe(201)
  privateDocId = JSON.parse(docText).data.id

  // The open session's recording row: LIVE_CLASS is a reserved origin the API refuses
  // from clients (anti-squatting rule), so the fixture is written exactly the way the
  // recording hand-off would write it — every read path is still exercised for real.
  openRecordingId = psql(
    `INSERT INTO media_assets (institution_id, title, description, media_type, file_url, status, visibility, source_type, source_id, teacher_id, is_deleted) ` +
      `VALUES ('${DMS}', '${openRecordingTitle}', 'E2E open learning recording', 'RECORDING', ` +
      `'https://cdn.test/e2e-open-${SFX}.mp4', 'READY', 'INSTITUTION', 'LIVE_CLASS', '${openClassId}', ` +
      `(SELECT id FROM users WHERE email = '${HOST_TEACHER.email}'), false) RETURNING id;`
  )
  expect(openRecordingId, "open recording fixture id").toMatch(/^[0-9a-f-]{36}$/)

  await apiRequest.dispose()
})

test.afterAll(async () => {
  // Delete only what this run created; an API that refuses is reported rather than
  // worked around — pre-existing records are never touched.
  for (const [id, label] of [
    [openClassId, "open session"],
    [privateClassId, "private session"],
  ] as Array<[string, string]>) {
    if (!id) continue
    const res = await asHost(`/v1/teachers/me/live-classes/${id}`, { method: "DELETE" }).catch(() => null)
    console.log(
      `[cleanup] cancel ${label} ${id} -> ${res ? res.status : "n/a"} ` +
        `(the API's DELETE soft-cancels; cancelled rows stay for audit but are excluded from discovery)`
    )
  }
  if (independentClassId && independentTeacherToken) {
    const res = await fetch(`${API}/v1/teachers/me/live-classes/${independentClassId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${independentTeacherToken}` },
    }).catch(() => null)
    console.log(`[cleanup] cancel independent session ${independentClassId} -> ${res ? res.status : "n/a"}`)
  }
  if (privateDocId && hostToken) {
    const res = await asHost(`/v1/media/${privateDocId}`, { method: "DELETE" }).catch(() => null)
    console.log(`[cleanup] delete private media ${privateDocId} -> ${res ? res.status : "n/a"}`)
  }
  if (openRecordingId && existsSync(PSQL)) {
    try {
      psql(`DELETE FROM media_assets WHERE id = '${openRecordingId}';`)
      console.log(`[cleanup] deleted open recording fixture ${openRecordingId}`)
    } catch (e) {
      console.log(`[cleanup] open recording fixture ${openRecordingId} NOT deleted: ${e}`)
    }
  }
  console.log(`[data] run-suffixed accounts left in place (repo precedent): ${otherSchoolEmail}, ${independentEmail}, ${independentTeacherEmail}, ${affiliatedEmail}`)
})

// ── scenario 11a: Platform Admin provisions an independent teacher through the UI ────────

test("Platform Admin form provisions an independent teacher with no school attached", async ({ page }) => {
  await seedAuth(page, adminToken, adminUser)
  await page.goto("/dashboard/platform-admin/users")
  await expect(page.getByRole("heading", { name: "Users", level: 1 })).toBeVisible({ timeout: 30000 })

  await page.getByRole("button", { name: "Add User" }).click()
  const modal = page.locator("div.fixed.inset-0.z-50")
  await expect(modal).toBeVisible()

  // The option exists only for the TEACHER role: hidden for the default role.
  const roleSelect = modal.locator('select:has(option[value="TEACHER"])')
  await expect(modal.getByText("Independent teacher (no school)")).toHaveCount(0)
  await roleSelect.selectOption("TEACHER")
  await expect(modal.getByText("Independent teacher (no school)")).toBeVisible()
  // Institutions must be loaded before the negative submit below means anything.
  await expect(modal.locator("#user-form-institution")).toBeEnabled()

  await modal.getByPlaceholder("John").fill("E2E")
  await modal.getByPlaceholder("Doe").fill(`Independent${SFX.slice(-4)}`)
  await modal.getByPlaceholder("user@example.com").fill(independentTeacherEmail)
  await modal.getByPlaceholder("Min 8 characters").fill(TEST_PASSWORD)

  // School requirement stays enforced while the box is off, on three layers. First the
  // control itself: `required` puts the select in the native value-missing state, so the
  // browser blocks submission before any request leaves the client (the i18n validation
  // message in handleSubmit is the second line of defense behind that).
  const createRequests: string[] = []
  page.on("request", (req) => {
    if (req.method() === "POST" && req.url().includes("/v1/platform-admin/users")) createRequests.push(req.url())
  })
  expect(
    await modal
      .locator("#user-form-institution")
      .evaluate((el) => (el as HTMLSelectElement).validity.valueMissing),
    "organization is in the native required (value-missing) state"
  ).toBe(true)

  await modal.getByRole("button", { name: "Create User" }).click()
  await page.waitForTimeout(500)
  expect(createRequests, "no create request may leave the client without a school").toHaveLength(0)
  await expect(modal.getByPlaceholder("user@example.com"), "the form must stay open").toBeVisible()

  // Second, the server: a school-less TEACHER without the explicit independent flag is
  // refused outright (never silently admitted).
  const bare = await asAdmin("/v1/platform-admin/users", {
    method: "POST",
    body: JSON.stringify({
      firstName: "Bare",
      lastName: `Teacher${SFX.slice(-4)}`,
      email: `e2e-bare-${SFX}@test.com`,
      password: TEST_PASSWORD,
      role: "TEACHER",
    }),
  })
  const bareText = await bare.text()
  expect(bare.status, `school-less teacher without the flag: ${bareText}`).toBe(400)

  // Checking the box makes the school optional AND disables the selector: nothing can
  // be attached by accident.
  await modal.getByRole("checkbox", { name: /Independent teacher/ }).check()
  await expect(modal.locator("#user-form-institution")).toBeDisabled()

  await modal.getByRole("button", { name: "Create User" }).click()
  await expect(modal.getByPlaceholder("user@example.com")).toHaveCount(0, { timeout: 20000 })

  // The list finds the new account, and the server persisted it WITHOUT a school.
  await page.getByPlaceholder("Search by name or email...").fill(independentTeacherEmail)
  await expect(page.getByText(independentTeacherEmail).first()).toBeVisible({ timeout: 15000 })

  const listRes = await asAdmin(`/v1/platform-admin/users?search=${encodeURIComponent(independentTeacherEmail)}`)
  expect(listRes.ok, `users search: ${listRes.status}`).toBeTruthy()
  const rows = (await listRes.json()).data?.content ?? []
  const row = rows.find((u: Record<string, unknown>) => u.email === independentTeacherEmail)
  expect(row, "created teacher in server list").toBeTruthy()
  expect(row.role).toBe("TEACHER")
  expect(row.institutionId ?? null, "independent teacher must persist with NO institution").toBeNull()
})

// ── scenario 11b: the same form still requires a school for a normal teacher ─────────────

test("Platform Admin form still requires a school for a normal teacher", async ({ page }) => {
  await seedAuth(page, adminToken, adminUser)
  await page.goto("/dashboard/platform-admin/users")
  await expect(page.getByRole("heading", { name: "Users", level: 1 })).toBeVisible({ timeout: 30000 })
  await page.getByRole("button", { name: "Add User" }).click()
  const modal = page.locator("div.fixed.inset-0.z-50")
  const roleSelect = modal.locator('select:has(option[value="TEACHER"])')
  await roleSelect.selectOption("TEACHER")

  // Option present but OFF, and the organization stays a hard requirement.
  await expect(modal.getByText("Independent teacher (no school)")).toBeVisible()
  await expect(modal.getByRole("checkbox", { name: /Independent teacher/ })).not.toBeChecked()
  await expect(modal.locator("#user-form-institution")).toBeEnabled()

  await modal.getByPlaceholder("John").fill("E2E")
  await modal.getByPlaceholder("Doe").fill(`Affiliated${SFX.slice(-4)}`)
  await modal.getByPlaceholder("user@example.com").fill(affiliatedEmail)
  await modal.getByPlaceholder("Min 8 characters").fill(TEST_PASSWORD)

  // Same enforcement as 11a: the empty organization keeps the select in the native
  // value-missing state, the browser blocks the submit, and nothing is sent — the form
  // stays open until a school is chosen.
  expect(
    await modal
      .locator("#user-form-institution")
      .evaluate((el) => (el as HTMLSelectElement).validity.valueMissing),
    "organization is in the native required (value-missing) state"
  ).toBe(true)
  await modal.getByRole("button", { name: "Create User" }).click()
  await page.waitForTimeout(400)
  await expect(modal.getByPlaceholder("user@example.com"), "the form must stay open").toBeVisible()

  await modal.locator("#user-form-institution").selectOption(DMS)
  await modal.getByRole("button", { name: "Create User" }).click()
  await expect(modal.getByPlaceholder("user@example.com")).toHaveCount(0, { timeout: 20000 })

  const listRes = await asAdmin(`/v1/platform-admin/users?search=${encodeURIComponent(affiliatedEmail)}`)
  expect(listRes.ok, `affiliated search: ${listRes.status}`).toBeTruthy()
  const rows = (await listRes.json()).data?.content ?? []
  const row = rows.find((u: Record<string, unknown>) => u.email === affiliatedEmail)
  expect(row, "affiliated teacher in server list").toBeTruthy()
  expect(row.institutionId, "school-affiliated teacher keeps the chosen school").toBe(DMS)
})

// ── scenario 3: the independent teacher signs in for real and manages her session ────────

test("independent teacher signs in with the real login form and manages her session with no school", async ({
  page,
  request,
}) => {
  // Real UI authentication for the account the form above created (no MFA on a fresh
  // account, so the plain credentials flow applies).
  await page.goto("/login")
  await page
    .locator("input[type='email'], input[name='email'], [placeholder*='email' i]")
    .first()
    .fill(independentTeacherEmail)
  await page.locator("input[type='password'], input[name='password']").first().fill(TEST_PASSWORD)
  await page.locator("button[type='submit'], button:has-text('Sign in'), button:has-text('Login')").first().click()
  await page.waitForURL(/\/dashboard/, { timeout: 30000 })

  await page.goto("/dashboard/teacher")
  await expect(page.getByText("Independent teacher").first()).toBeVisible({ timeout: 30000 })

  // A token + user object from the same real credentials, for the API half of this test
  // and for the no-tenant media steps later on (her account has NO institution at all).
  const herLogin = await login(request, independentTeacherEmail, TEST_PASSWORD)
  independentTeacherToken = herLogin.token
  independentTeacherUser = herLogin.user
  expect(independentTeacherUser.institutionId ?? null, "independent teacher has no institution").toBeNull()

  // She creates a session with NO institution header at all. Asking for an INSTITUTION
  // audience while school-less must be normalised to the open audience — never someone
  // else's scope.
  const create = await fetch(`${API}/v1/teachers/me/live-classes`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${independentTeacherToken}` },
    body: JSON.stringify({
      title: independentSessionTitle,
      description: "E2E session hosted without a school",
      sessionType: "LECTURE",
      scheduledAt: localStamp(8),
      durationMinutes: 45,
      maxParticipants: 30,
      enableRecording: false,
      accessLevel: "INSTITUTION",
    }),
  })
  const createText = await create.text()
  expect(create.status, `independent create: ${createText}`).toBe(201)
  const createdBody = JSON.parse(createText).data
  independentClassId = createdBody.id
  expect(createdBody.accessLevel, "school-less host: INSTITUTION audience must be forced PUBLIC").toBe("PUBLIC")
  expect(createdBody.publicationStatus).toBe("DRAFT")

  // Scenario 7 (server half) while the class is still DRAFT — so state can never be the
  // refusal reason: another teacher may not act on it. Ownership is enforced against the
  // class's own teacher, and ForbiddenException maps to 403.
  const foreignSubmit = await fetch(`${API}/v1/teachers/me/live-classes/${independentClassId}/submit-for-approval`, {
    method: "POST",
    headers: { Authorization: `Bearer ${hostToken}`, "X-Institution-Id": DMS },
  })
  const foreignText = await foreignSubmit.text()
  console.log(`[evidence] foreign submit-for-approval answered ${foreignSubmit.status}: ${foreignText}`)
  expect(foreignSubmit.status, "a teacher acting on another teacher's session must be forbidden").toBe(403)

  // Her own path stays open: submit -> platform-admin approve -> publish.
  const submit = await fetch(`${API}/v1/teachers/me/live-classes/${independentClassId}/submit-for-approval`, {
    method: "POST",
    headers: { Authorization: `Bearer ${independentTeacherToken}` },
  })
  const submitText = await submit.text()
  expect(submit.ok, `independent submit: ${submit.status} ${submitText}`).toBeTruthy()

  const approve = await asAdmin(`/v1/platform-admin/live-classes/governance/${independentClassId}/approve`, {
    method: "POST",
    body: JSON.stringify({ note: "E2E independent host" }),
  })
  const approveText = await approve.text()
  expect(approve.ok, `independent approve: ${approve.status} ${approveText}`).toBeTruthy()

  const publish = await asAdmin(`/v1/platform-admin/live-classes/governance/${independentClassId}/publish`, {
    method: "POST",
  })
  const publishText = await publish.text()
  expect(publish.ok, `independent publish: ${publish.status} ${publishText}`).toBeTruthy()

  // Management surface: her own list shows the session — no school context anywhere.
  await page.goto("/dashboard/teacher/live-classes")
  await expect(page.getByText(independentSessionTitle).first()).toBeVisible({ timeout: 30000 })
})

// ── scenarios 1/2/4/6: discovery matrix — host school, another school, no school ─────────

test("the published open session reaches learners of the host school, another school and no school — the private session only reaches its own school", async ({
  page,
}) => {
  const learners: Array<{
    label: string
    token: string
    user: Record<string, unknown>
    seesPrivate: boolean
  }> = [
    { label: "host-school learner", token: studentToken, user: studentUser, seesPrivate: true },
    { label: "another-school learner", token: otherSchoolToken, user: otherSchoolUser, seesPrivate: false },
    { label: "independent learner", token: independentToken, user: independentUser, seesPrivate: false },
  ]

  for (const learner of learners) {
    await test.step(learner.label, async () => {
      await seedAuth(page, learner.token, learner.user)
      await page.goto("/dashboard/learner/live-classes")
      await expect(
        page.getByText(openTitle).first(),
        `${learner.label} must discover the published open session`
      ).toBeVisible({ timeout: 30000 })
      if (learner.seesPrivate) {
        await expect(
          page.getByText(privateTitle).first(),
          `${learner.label} must see their own school's private session`
        ).toBeVisible({ timeout: 15000 })
      } else {
        await expect(
          page.getByText(privateTitle),
          `${learner.label} must NOT see the school-private session`
        ).toHaveCount(0)
      }
    })
  }

  // Server side agrees with the UI on the direct-detail routes (scenario 6 denial).
  const foreignDetail = await fetch(`${API}/v1/learner/live-classes/${privateClassId}`, {
    headers: { Authorization: `Bearer ${independentToken}` },
  })
  const foreignText = await foreignDetail.text()
  console.log(`[evidence] private detail for independent learner answered ${foreignDetail.status}`)
  expect(foreignDetail.status, "private session detail must be refused for outsiders").toBe(404)
  expect(foreignText, "no private metadata leaks with the refusal").not.toContain(privateTitle)

  const otherDetail = await fetch(`${API}/v1/learner/live-classes/${openClassId}`, {
    headers: { Authorization: `Bearer ${otherSchoolToken}`, "X-Institution-Id": MPA },
  })
  expect(otherDetail.ok, `another school reaches the open session detail: ${otherDetail.status}`).toBeTruthy()
  const otherBody = await otherDetail.json()
  expect(otherBody.data.title).toBe(openTitle)
})

// ── scenario 5: placement retained ──────────────────────────────────────────────────────

test("the school learner keeps their own school placement while open learning exists", async ({ page }) => {
  await seedAuth(page, studentToken, studentUser)
  await page.goto("/dashboard/learner")
  await expect(
    page.getByText("Dar es Salaam Model School").first(),
    "open learning must never displace the learner's own school"
  ).toBeVisible({ timeout: 30000 })
})

/**
 * The page's real error surfaces.
 *
 * Next.js injects a visually-hidden `#__next-route-announcer__` with `role="alert"` on every
 * hydrated page (it announces route changes to screen readers). It is infrastructure, not a
 * failure, so it is excluded — anything else with role="alert" on these pages is a genuine
 * error banner (the media fetch error surface).
 */
const errorSurfaces = (page: Page) => page.locator('[role="alert"]:not(#__next-route-announcer__)')

// ── scenario 8 (browser half): media listing per audience, no error surface ──────────────

test("media library shows each audience exactly what they may see, without an error surface", async ({
  page,
}) => {
  // Self-registered learner (platform-level: registration links a real HQ membership when
  // HQ exists — no school either way): the library loads cleanly, shows the open
  // recording, never the private document.
  await seedAuth(page, independentToken, independentUser)
  await page.goto("/dashboard/learner/media-library")
  await expect(page.getByRole("heading", { name: "Media Library", level: 1 })).toBeVisible({ timeout: 30000 })
  await expect(errorSurfaces(page), "no error surface for the registered learner").toHaveCount(0)
  await expect(page.getByText(openRecordingTitle).first()).toBeVisible({ timeout: 20000 })
  await expect(page.getByText(privateDocTitle)).toHaveCount(0)

  // Independent teacher: NO institution at all (the reader class that used to be refused
  // server-side). Her own media scope renders without an error surface and never exposes
  // another school's document.
  await seedAuth(page, independentTeacherToken, independentTeacherUser)
  await page.goto("/dashboard/teacher/media-library")
  await expect(page.getByRole("heading", { name: "Media Library", level: 1 })).toBeVisible({ timeout: 30000 })
  await expect(errorSurfaces(page), "no error surface for a reader with no tenant").toHaveCount(0)
  await expect(page.getByText(privateDocTitle)).toHaveCount(0)

  // Host-school learner sees both: their own private document and the open recording.
  await seedAuth(page, studentToken, studentUser)
  await page.goto("/dashboard/learner/media-library")
  await expect(page.getByText(privateDocTitle).first()).toBeVisible({ timeout: 30000 })
  await expect(page.getByText(openRecordingTitle).first()).toBeVisible({ timeout: 30000 })

  // Another school: the open recording crosses institutions, the private one does not.
  await seedAuth(page, otherSchoolToken, otherSchoolUser)
  await page.goto("/dashboard/learner/media-library")
  await expect(page.getByText(openRecordingTitle).first()).toBeVisible({ timeout: 30000 })
  await expect(page.getByText(privateDocTitle)).toHaveCount(0)
})

// ── scenario 8 (server half): detail + download-url agree with the listing ───────────────

test("detail and download-url answer every audience the same way the listing does", async () => {
  const open = openRecordingId
  const priv = privateDocId

  // The open recording: reachable by all three readers; entitlement resolves to the
  // open-learning source, not to a tenant claim.
  for (const [label, token, header] of [
    ["host-school learner", studentToken, DMS],
    ["another-school learner", otherSchoolToken, MPA],
    ["independent learner", independentToken, ""],
  ] as Array<[string, string, string]>) {
    const headers: Record<string, string> = { Authorization: `Bearer ${token}` }
    if (header) headers["X-Institution-Id"] = header

    const detail = await fetch(`${API}/v1/media/${open}`, { headers })
    expect(detail.ok, `${label} open detail: ${detail.status}`).toBeTruthy()
    const detailBody = (await detail.json()).data
    expect(detailBody.id).toBe(open)
    expect(detailBody.title).toBe(openRecordingTitle)

    const dl = await fetch(`${API}/v1/media/${open}/download-url`, { headers })
    expect(dl.ok, `${label} open download-url: ${dl.status}`).toBeTruthy()
    const dlBody = (await dl.json()).data
    expect(dlBody.assetId).toBe(open)
  }

  // The private document: only the host school reaches it.
  const hostDetail = await fetch(`${API}/v1/media/${priv}`, {
    headers: { Authorization: `Bearer ${studentToken}`, "X-Institution-Id": DMS },
  })
  expect(hostDetail.ok, `host private detail: ${hostDetail.status}`).toBeTruthy()
  expect((await hostDetail.json()).data.id).toBe(priv)
  const hostDl = await fetch(`${API}/v1/media/${priv}/download-url`, {
    headers: { Authorization: `Bearer ${studentToken}`, "X-Institution-Id": DMS },
  })
  expect(hostDl.ok, `host private download-url: ${hostDl.status}`).toBeTruthy()
  expect((await hostDl.json()).data.assetId).toBe(priv)

  for (const [label, token, header] of [
    ["another-school learner", otherSchoolToken, MPA],
    ["independent learner", independentToken, ""],
  ] as Array<[string, string, string]>) {
    const headers: Record<string, string> = { Authorization: `Bearer ${token}` }
    if (header) headers["X-Institution-Id"] = header

    const detail = await fetch(`${API}/v1/media/${priv}`, { headers })
    console.log(`[evidence] private detail for ${label} answered ${detail.status}`)
    expect(detail.status, `${label} private detail must be refused`).toBe(404)

    const dl = await fetch(`${API}/v1/media/${priv}/download-url`, { headers })
    console.log(`[evidence] private download-url for ${label} answered ${dl.status}`)
    expect(dl.status, `${label} private download-url must be refused`).toBe(404)
  }

  // The institution-less teacher reaches HER OWN media scope with no tenant header at
  // all (`/v1/media/my` used to deny readers without an institution outright).
  const myMedia = await fetch(`${API}/v1/media/my`, {
    headers: { Authorization: `Bearer ${independentTeacherToken}` },
  })
  const myMediaText = await myMedia.text()
  console.log(`[evidence] /v1/media/my for institution-less teacher answered ${myMedia.status}`)
  expect(myMedia.ok, `own media scope for a reader with no tenant: ${myMedia.status} ${myMediaText}`).toBeTruthy()
  expect(myMediaText, "her own scope never contains another school's document").not.toContain(privateDocTitle)
})

// ── scenario 7: scope denial outside the classroom surface ───────────────────────────────

test("a teacher cannot publish outside their own scope, and the UI never shows foreign sessions to them", async ({
  page,
}) => {
  // Server side: the platform gate is hasRole('ADMIN'); a teacher's token is refused
  // even though the session exists.
  const foreignPublish = await fetch(
    `${API}/v1/platform-admin/live-classes/governance/${independentClassId}/publish`,
    { method: "POST", headers: { Authorization: `Bearer ${hostToken}`, "X-Institution-Id": DMS } }
  )
  const foreignText = await foreignPublish.text()
  console.log(`[evidence] teacher publish attempt answered ${foreignPublish.status}: ${foreignText}`)
  expect(foreignPublish.status, "publishing another scope's session must be forbidden").toBe(403)

  // Browser side: the other teacher's session is simply not on their management surface
  // (the list is scoped to the caller's own classes).
  await seedAuth(page, hostToken, hostUser)
  await page.goto("/dashboard/teacher/live-classes")
  await expect(page.getByText(independentSessionTitle)).toHaveCount(0)
})

// ── scenario 12: English and Swahili both work ───────────────────────────────────────────

test("the interface works in English and Swahili", async ({ page }) => {
  // Public switcher (common.changeLanguage): EN -> SW -> EN round trip.
  await page.goto("/")
  const switcher = page.locator('button[aria-label="Change language"], button[aria-label="Badilisha lugha"]').first()
  await expect(switcher).toBeVisible({ timeout: 30000 })
  await expect(switcher).toHaveText("EN")

  await switcher.click()
  await expect(switcher).toHaveText("SW")
  await expect(page.locator('button[aria-label="Change language"]')).toHaveCount(0)

  // The choice persists (NEXT_LOCALE cookie + elmkusoma_language storage) into the
  // authenticated app: the media library renders its Swahili copy
  // (highered.mediaLibrary.title) and English again after switching back.
  await seedAuth(page, studentToken, studentUser)
  await page.goto("/dashboard/learner/media-library")
  await expect(page.getByRole("heading", { name: "Maktaba ya Vyombo", level: 1 })).toBeVisible({ timeout: 30000 })

  await page.goto("/")
  await page.locator('button[aria-label="Badilisha lugha"]').first().click()
  await expect(page.locator('button[aria-label="Change language"]').first()).toBeVisible()
  await page.goto("/dashboard/learner/media-library")
  await expect(page.getByRole("heading", { name: "Media Library", level: 1 })).toBeVisible({ timeout: 30000 })
})
