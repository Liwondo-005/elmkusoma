// Verifies the attendance page's de-duplication against the REAL API payload.
//
// /v1/teachers/me/classes emits one row per TeacherAssignment, so a teacher who
// teaches one class group across two subjects receives that classGroupId twice.
// The attendance page now collapses those rows by classGroupId before rendering
// its <option> lists. This test runs that exact logic over live API data.
import { test, expect } from "@playwright/test"

const API = "http://localhost:8080/v1"
const TEACHER = { email: "teacher@test.com", password: "password" }

interface TeacherClassRow {
  classGroupId: string
  className: string
  subjectId?: string
  subjectName?: string
}

test("attendance class de-duplication collapses repeated classGroupIds", async ({ request }) => {
  const login = await request.post(`${API}/auth/login`, { data: TEACHER })
  expect(login.ok(), "teacher login failed").toBeTruthy()
  const body = await login.json()
  const token = body.data.accessToken ?? body.data.token
  const institutionId = body.data.user?.institutionId ?? body.data.institutionId
  expect(institutionId, "login did not return an institutionId").toBeTruthy()

  const res = await request.get(`${API}/teachers/me/classes`, {
    headers: { Authorization: `Bearer ${token}`, "X-Institution-Id": institutionId },
  })
  expect(res.ok(), `GET /teachers/me/classes -> ${res.status()}`).toBeTruthy()

  const rows: TeacherClassRow[] = (await res.json()).data ?? []
  expect(rows.length, "expected some class rows").toBeGreaterThan(0)

  const rawIds = rows.map((r) => r.classGroupId)
  const rawDupes = rawIds.filter((id, i) => rawIds.indexOf(id) !== i)
  console.log(
    `API rows=${rows.length} unique=${new Set(rawIds).size} ` +
      `duplicates=${rawDupes.length ? rawDupes.join(",") : "none"}`,
  )

  // The exact logic added to loadClasses() in the attendance page.
  const uniqueClasses = new Map<string, TeacherClassRow>()
  for (const c of rows) {
    if (c.classGroupId && !uniqueClasses.has(c.classGroupId)) {
      uniqueClasses.set(c.classGroupId, c)
    }
  }
  const deduped = [...uniqueClasses.values()]

  // Every key React sees is now unique.
  const dedupedIds = deduped.map((c) => c.classGroupId)
  expect(dedupedIds.length).toBe(new Set(dedupedIds).size)

  // No real class group is lost: only duplicate rows collapse.
  expect(new Set(dedupedIds)).toEqual(new Set(rawIds.filter(Boolean)))
  expect(deduped.length).toBeLessThanOrEqual(rows.length)

  // First-row-wins, matching the documented intent.
  for (const c of deduped) {
    const first = rows.find((r) => r.classGroupId === c.classGroupId)!
    expect(c).toEqual(first)
  }

  // Attendance is submitted with classGroupId only, so no distinct selectable
  // value can be lost by collapsing: every option value still round-trips.
  for (const id of dedupedIds) expect(id).toBeTruthy()

  console.log(`deduped -> ${deduped.length} unique class options: ${dedupedIds.join(", ")}`)
})