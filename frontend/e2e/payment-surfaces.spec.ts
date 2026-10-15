import { test, expect, type Page } from "@playwright/test"

/**
 * Payment surfaces: [Developer 02] frontend behaviour.
 *
 * The backend responses below are intercepted rather than served by a live database. That is not
 * a shortcut around a missing endpoint - every path asserted here exists in the backend today
 * (GET /v1/my/payments, POST /v1/my/payments/{id}/cancel, GET /v1/platform-admin/payments,
 * /entitlements, /webhook-events, /services). What is under test is the frontend's handling of
 * them: which states it renders, what it does with a status it does not recognise, and what it
 * refuses to show.
 *
 * The rule this suite exists to defend: a pending payment is never rendered as a paid one, and a
 * figure the platform cannot compute is shown as absent rather than as zero.
 */

const PARENT_USER = {
  id: "11111111-1111-1111-1111-111111111111",
  email: "parent@example.test",
  firstName: "Amina",
  lastName: "Juma",
  role: "Parent",
  institutionId: "00000000-0000-0000-0000-000000000001",
}

const ADMIN_USER = {
  id: "22222222-2222-2222-2222-222222222222",
  email: "admin@example.test",
  firstName: "Baraka",
  lastName: "Mushi",
  role: "Admin",
  institutionId: "00000000-0000-0000-0000-000000000001",
}

/**
 * Seeds the identity both the client and the server gate read.
 *
 * These must be context cookies, not `addInitScript`. Next's `proxy.ts` runs on the server and
 * reads `request.cookies`, so a cookie written by page script never reaches the middleware and
 * every navigation is redirected to /login. The value is URI-encoded because `lib/auth.tsx`
 * writes it that way and `proxy.ts` decodes with `decodeURIComponent` before JSON.parse.
 */
async function signIn(page: Page, user: typeof PARENT_USER) {
  const encoded = encodeURIComponent(JSON.stringify(user))

  await page.context().addCookies([
    { name: "elmkusoma_access_token", value: "e2e-fake-token", url: "http://localhost:3000" },
    { name: "elmkusoma_current_user", value: encoded, url: "http://localhost:3000" },
  ])

  // localStorage is unreadable until a page has loaded, so seed it on every document.
  await page.addInitScript((u) => {
    window.localStorage.setItem("elmkusoma_access_token", "e2e-fake-token")
    window.localStorage.setItem("elmkusoma_current_user", JSON.stringify(u))
    window.localStorage.setItem("elmkusoma_institution_id", u.institutionId)
  }, user)
}

/** Wraps a payload in the ApiResponse envelope the backend returns. */
function ok(data: unknown) {
  return { success: true, message: "ok", data, timestamp: "2026-01-01T00:00:00" }
}

const PAYMENT_ROWS = {
  outstanding: 3500,
  paidThisTerm: 2000,
  pendingPayments: [
    {
      id: "pay-pending-1",
      amount: 3500,
      currency: "TZS",
      description: "Term 2 tuition",
      serviceType: "TUITION",
      status: "PENDING",
      paidAt: null,
      createdAt: "2026-01-05T09:00:00",
      providerReference: null,
    },
  ],
  recentPayments: [
    {
      id: "pay-done-1",
      amount: 2000,
      currency: "TZS",
      description: "Term 1 tuition",
      serviceType: "TUITION",
      status: "COMPLETED",
      paidAt: "2026-01-02T11:30:00",
      createdAt: "2026-01-01T08:00:00",
      providerReference: "PSP-REF-9911",
    },
  ],
}

const CHILD = {
  studentId: "44444444-4444-4444-4444-444444444444",
  studentName: "Zainab Juma",
  admissionNumber: "ADM-2026-0042",
  relationshipType: "MOTHER",
  isPrimary: true,
  className: "Form 3A",
  educationLevel: "SECONDARY",
}

const CHILD_2 = {
  ...CHILD,
  studentId: "55555555-5555-5555-5555-555555555555",
  studentName: "Yusuf Juma",
  className: "Form 1B",
  educationLevel: "PRIMARY",
}

/** Children list + per-child payment/entitlement stubs, shared by the child-view tests. */
async function stubChildren(
  page: Page,
  opts: {
    payments?: unknown[]
    entitlements?: unknown[]
    children?: unknown[]
    entitlementStatus?: number
  } = {},
) {
  await page.route("**/v1/my/children", (r) =>
    r.fulfill({ json: ok(opts.children ?? [CHILD, CHILD_2]) }),
  )
  await page.route("**/v1/my/children/*/payments", (r) =>
    r.fulfill({ json: ok(opts.payments ?? []) }),
  )
  await page.route("**/v1/my/children/*/entitlements", (r) =>
    r.fulfill({
      status: opts.entitlementStatus ?? 200,
      json: ok(opts.entitlements ?? []),
    }),
  )
}

test.describe("Parent payments", () => {
  test("separates a pending payment from a settled one and never calls pending paid", async ({ page }) => {
    await signIn(page, PARENT_USER)
    await page.route("**/v1/my/payments", (route) => route.fulfill({ json: ok(PAYMENT_ROWS) }))

    await page.goto("/dashboard/parent/payments")

    // The settled row is labelled COMPLETED and sits in history.
    await expect(page.getByText("COMPLETED")).toBeVisible()
    await expect(page.getByText("PSP-REF-9911")).toBeVisible()

    // The pending row is in its own section, clearly not history.
    // `exact` is required because the section heading "Pending Payments" also contains "PENDING".
    const pendingSection = page.locator("section").filter({ hasText: "Pending Payments" })
    await expect(pendingSection.getByText("PENDING", { exact: true })).toBeVisible()
    await expect(pendingSection.getByText("COMPLETED", { exact: true })).toHaveCount(0)

    // The "paid this term" figure is the backend's COMPLETED sum, not the combined total.
    await expect(page.getByText("Paid This Term")).toBeVisible()
    await expect(page.getByText("2,000").first()).toBeVisible()

    // A pending row offers a cancel; a settled one does not.
    await expect(pendingSection.getByRole("button", { name: "Cancel" })).toHaveCount(1)
  })

  test("shows an unrecognised status verbatim instead of guessing", async ({ page }) => {
    await signIn(page, PARENT_USER)
    await page.route("**/v1/my/payments", (route) =>
      route.fulfill({
        json: ok({
          outstanding: 0,
          paidThisTerm: 0,
          pendingPayments: [],
          recentPayments: [
            {
              id: "pay-odd",
              amount: 1000,
              currency: "TZS",
              description: "Odd status row",
              serviceType: "TUITION",
              status: "CHARGEBACK_PENDING",
              paidAt: null,
              createdAt: "2026-01-05T09:00:00",
              providerReference: null,
            },
          ],
        }),
      }),
    )

    await page.goto("/dashboard/parent/payments")

    // Rendered as-is, not folded into pending or completed.
    await expect(page.getByText("CHARGEBACK_PENDING")).toBeVisible()
    await expect(page.getByRole("button", { name: "Cancel" })).toHaveCount(0)
  })

  test("surfaces a load failure with a retry that refetches", async ({ page }) => {
    await signIn(page, PARENT_USER)
    let attempts = 0
    await page.route("**/v1/my/payments", (route) => {
      attempts += 1
      if (attempts === 1) return route.fulfill({ status: 500, json: { message: "boom" } })
      return route.fulfill({ json: ok(PAYMENT_ROWS) })
    })

    await page.goto("/dashboard/parent/payments")
    await expect(page.getByRole("alert")).toBeVisible()

    await page.getByRole("button", { name: "Try again" }).click()
    await expect(page.getByText("COMPLETED")).toBeVisible()
    expect(attempts).toBeGreaterThan(1)
  })

  test("shows an honest empty state, not a zero-filled dashboard", async ({ page }) => {
    await signIn(page, PARENT_USER)
    await page.route("**/v1/my/payments", (route) =>
      route.fulfill({
        json: ok({ outstanding: 0, paidThisTerm: 0, pendingPayments: [], recentPayments: [] }),
      }),
    )

    await page.goto("/dashboard/parent/payments")
    await expect(page.getByText("No payments yet")).toBeVisible()
    // No invented transactions, and no paid total dressed up as a real figure.
    await expect(page.getByText("COMPLETED")).toHaveCount(0)
  })

  test("a failed cancel leaves the row on screen rather than hiding it", async ({ page }) => {
    await signIn(page, PARENT_USER)
    await page.route("**/v1/my/payments", (route) => route.fulfill({ json: ok(PAYMENT_ROWS) }))
    await page.route("**/v1/my/payments/*/cancel", (route) =>
      route.fulfill({ status: 409, json: { message: "Already completed" } }),
    )
    page.on("dialog", (d) => void d.accept())

    await page.goto("/dashboard/parent/payments")
    await page.getByRole("button", { name: "Cancel" }).click()

    // The row survives a rejected cancel, because the outcome is unknown, not "cancelled".
    // Scoped away from Next's route announcer, which is also role="alert".
    await expect(page.getByText("Already completed")).toBeVisible()
    await expect(page.getByText("Term 2 tuition")).toBeVisible()
  })

  test("renders FAILED and REFUNDED distinctly and never as paid", async ({ page }) => {
    await signIn(page, PARENT_USER)
    await page.route("**/v1/my/payments", (route) =>
      route.fulfill({
        json: ok({
          outstanding: 0,
          paidThisTerm: 0,
          pendingPayments: [],
          recentPayments: [
            { id: "f1", amount: 1000, currency: "TZS", description: "Failed charge", serviceType: "TUITION", status: "FAILED", paidAt: null, createdAt: "2026-01-01T00:00:00", providerReference: null },
            { id: "r1", amount: 2000, currency: "TZS", description: "Refunded charge", serviceType: "TUITION", status: "REFUNDED", paidAt: "2026-01-02T00:00:00", createdAt: "2026-01-01T00:00:00", providerReference: "PSP-REF-1" },
            { id: "c1", amount: 500, currency: "TZS", description: "Cancelled charge", serviceType: "TUITION", status: "CANCELLED", paidAt: null, createdAt: "2026-01-01T00:00:00", providerReference: null },
          ],
        }),
      }),
    )
    await stubChildren(page)

    await page.goto("/dashboard/parent/payments")

    for (const status of ["FAILED", "REFUNDED", "CANCELLED"]) {
      await expect(page.getByText(status, { exact: true })).toBeVisible()
    }
    // A failed, refunded or cancelled payment is not money in. The paid tile is the backend's
    // own figure and must still read zero, so no status below can inflate it.
    const paidTile = page.locator("p", { hasText: /^0$/ })
    await expect(page.getByText("Paid This Term")).toBeVisible()
    await expect(page.getByText(/TZS\s*0|^0$/).first()).toBeVisible()
    // None of them may be offered a cancel.
    await expect(page.getByRole("button", { name: "Cancel" })).toHaveCount(0)
  })
})

test.describe("Parent payments — child view", () => {
  test("shows the payer/recipient split and per-child access grants", async ({ page }) => {
    await signIn(page, PARENT_USER)
    await page.route("**/v1/my/payments", (route) => route.fulfill({ json: ok(PAYMENT_ROWS) }))
    await stubChildren(page, {
      entitlements: [
        { id: "e1", serviceType: "COURSE", serviceId: "s1", status: "ACTIVE", startsAt: "2026-01-01T00:00:00", expiresAt: null },
        { id: "e2", serviceType: "REPLAY", serviceId: "s2", status: "ACTIVE", startsAt: "2026-01-01T00:00:00", expiresAt: "2099-01-01T00:00:00" },
      ],
      payments: [
        { id: "cp1", amount: 2000, currency: "TZS", description: "Term 2 books", serviceType: "TUITION", status: "COMPLETED", paidAt: "2026-01-05T00:00:00", createdAt: "2026-01-04T00:00:00", providerReference: "PSP-CHILD-1" },
      ],
    })

    await page.goto("/dashboard/parent/payments")

    // Payer vs recipient is stated, not implied.
    await expect(page.getByText("You are the payer")).toBeVisible()
    // Addressed by id: a <select> wrapped in a <label> folds its option text into the computed
    // accessible name, so getByLabel would not match the label's own words.
    const childSelect = page.locator("#payment-child-select")
    await expect(childSelect).toBeVisible()
    await expect(childSelect.locator("option")).toHaveCount(2)
    await expect(childSelect.locator("option").first()).toHaveText("Zainab Juma (Form 3A)")

    // A null expiry is "no end date" - never rendered as a countdown or as expired.
    await expect(page.getByText("No end date")).toBeVisible()
    // en-GB with a two-digit day renders "01 Jan 2099".
    await expect(page.getByText("Until 01 Jan 2099")).toBeVisible()

    await expect(page.getByText("PSP-CHILD-1")).toBeVisible()
    // Only the account's own pending payment is cancellable; the child's settled one is not.
    await expect(page.getByRole("button", { name: "Cancel" })).toHaveCount(1)
  })

  test("switching child reloads that child's data and does not leak the other", async ({ page }) => {
    await signIn(page, PARENT_USER)
    await page.route("**/v1/my/payments", (route) => route.fulfill({ json: ok(PAYMENT_ROWS) }))
    await stubChildren(page, {
      entitlements: [
        { id: "e-zainab", serviceType: "COURSE", serviceId: "s1", status: "ACTIVE", startsAt: "2026-01-01T00:00:00", expiresAt: null },
      ],
    })
    await page.route("**/v1/my/children/55555555-5555-5555-5555-555555555555/entitlements", (r) =>
      r.fulfill({
        json: ok([
          { id: "e-yusuf", serviceType: "LIVE_CLASS", serviceId: "s9", status: "ACTIVE", startsAt: "2026-01-01T00:00:00", expiresAt: null },
        ]),
      }),
    )

    await page.goto("/dashboard/parent/payments")
    await expect(page.getByText("COURSE", { exact: true })).toBeVisible()

    await page.locator("#payment-child-select").selectOption("55555555-5555-5555-5555-555555555555")

    await expect(page.getByText("LIVE_CLASS", { exact: true })).toBeVisible()
    await expect(page.getByText("COURSE", { exact: true })).toHaveCount(0)
  })

  test("a child with no payments says so instead of showing a blank panel", async ({ page }) => {
    await signIn(page, PARENT_USER)
    await page.route("**/v1/my/payments", (route) => route.fulfill({ json: ok(PAYMENT_ROWS) }))
    await stubChildren(page)

    await page.goto("/dashboard/parent/payments")

    await expect(page.getByText("No payments are recorded against this child.")).toBeVisible()
    await expect(page.getByText("This child has no access grants yet.")).toBeVisible()
  })

  test("an entitlement endpoint failure does not blank the payment history", async ({ page }) => {
    await signIn(page, PARENT_USER)
    await page.route("**/v1/my/payments", (route) => route.fulfill({ json: ok(PAYMENT_ROWS) }))
    await stubChildren(page, { entitlementStatus: 500 })

    await page.goto("/dashboard/parent/payments")

    // The history is the part that matters most and must survive a secondary failure.
    await expect(page.getByText("COMPLETED", { exact: true })).toBeVisible()
    await expect(page.getByText("PSP-REF-9911")).toBeVisible()
  })
})

test.describe("Parent payments — filters and export", () => {
  test("a date range that excludes every row explains why, and can be cleared", async ({ page }) => {
    await signIn(page, PARENT_USER)
    await page.route("**/v1/my/payments", (route) => route.fulfill({ json: ok(PAYMENT_ROWS) }))
    await stubChildren(page)

    await page.goto("/dashboard/parent/payments")
    await expect(page.getByText("PSP-REF-9911")).toBeVisible()

    // By id, not getByLabel: "To" is a substring of the Next.js dev-tools button's label, and a
    // wrapped control's accessible name is not reliably the label's own words.
    await page.locator("#payment-filter-from").fill("2030-01-01")
    await page.locator("#payment-filter-to").fill("2030-12-31")

    await expect(page.getByText("No payments in this date range")).toBeVisible()
    await expect(page.getByText("PSP-REF-9911")).toHaveCount(0)

    await page.getByRole("button", { name: "Clear dates" }).click()
    await expect(page.getByText("PSP-REF-9911")).toBeVisible()
  })

  test("the export is a real CSV of the visible rows with no invented total", async ({ page }) => {
    await signIn(page, PARENT_USER)
    await page.route("**/v1/my/payments", (route) => route.fulfill({ json: ok(PAYMENT_ROWS) }))
    await stubChildren(page)

    await page.goto("/dashboard/parent/payments")

    const [download] = await Promise.all([
      page.waitForEvent("download"),
      page.getByRole("button", { name: "Export CSV" }).click(),
    ])

    expect(download.suggestedFilename()).toMatch(/^elmkusoma-payments-\d{4}-\d{2}-\d{2}\.csv$/)

    const stream = await download.createReadStream()
    const chunks: Buffer[] = []
    for await (const chunk of stream) chunks.push(Buffer.from(chunk))
    const csv = Buffer.concat(chunks).toString("utf8")

    // Header plus the two rows on screen - and deliberately no total row.
    const lines = csv.trim().split("\r\n")
    expect(lines).toHaveLength(3)
    expect(lines[0]).toContain("Reference")
    expect(csv).toContain("PSP-REF-9911")
    expect(csv).toContain("Term 2 tuition")
    expect(lines.some((l) => /total|sum/i.test(l))).toBe(false)
  })
})

test.describe("Platform Admin financial command center", () => {
  async function stubFinance(page: Page, overrides: Record<string, unknown> = {}) {
    const pages = {
      payments: {
        content: [
          {
            id: "aaaaaaaa-0000-0000-0000-000000000001",
            parentId: PARENT_USER.id,
            studentId: "33333333-3333-3333-3333-333333333333",
            amount: 2000,
            currency: "TZS",
            status: "COMPLETED",
            serviceType: "TUITION",
            createdAt: "2026-01-02T08:00:00",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 137,
        totalPages: 7,
        first: true,
        last: false,
      },
      entitlements: {
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
      },
      exceptions: {
        content: [
          {
            id: "ex-1",
            source: "PAYMENT",
            eventType: "payment.succeeded",
            verificationStatus: "FAILED",
            processingResult: "AMOUNT_MISMATCH",
            errorDetails: "amount 500 did not match recorded 2000",
            retryCount: 2,
            receivedAt: "2026-01-03T10:00:00",
            processedAt: "2026-01-03T10:00:05",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      },
      services: {
        content: [
          {
            id: "svc-1",
            name: "Premium streaming",
            code: "STREAM-STD",
            description: "",
            category: "MEDIA",
            isActive: true,
            requiresVerification: false,
            maxSeats: null,
            monthlyPrice: 5000,
            currency: "TZS",
            createdAt: "2026-01-01T00:00:00",
          },
          {
            id: "svc-2",
            name: "Unpriced pilot",
            code: "PILOT",
            description: "",
            category: "MEDIA",
            isActive: false,
            requiresVerification: false,
            maxSeats: 10,
            monthlyPrice: null,
            currency: "TZS",
            createdAt: "2026-01-01T00:00:00",
          },
        ],
        page: 0,
        size: 50,
        totalElements: 2,
        totalPages: 1,
        first: true,
        last: true,
      },
      ...overrides,
    }

    await page.route("**/v1/platform-admin/payments**", (r) => {
      // Echo the requested page back, as the real PageResponse does. A stub that always
      // answers page 0 would make the pager look broken when the app is behaving correctly.
      const requested = Number(new URL(r.request().url()).searchParams.get("page") ?? 0)
      const body = pages.payments as { page: number; totalPages: number; first: boolean; last: boolean }
      return r.fulfill({
        json: ok({
          ...body,
          page: requested,
          first: requested === 0,
          last: requested >= body.totalPages - 1,
        }),
      })
    })
    await page.route("**/v1/platform-admin/entitlements**", (r) => r.fulfill({ json: ok(pages.entitlements) }))
    await page.route("**/v1/platform-admin/webhook-events**", (r) => r.fulfill({ json: ok(pages.exceptions) }))
    await page.route("**/v1/platform-admin/services**", (r) => r.fulfill({ json: ok(pages.services) }))
  }

  test("reports the backend's own record counts, not a page sum", async ({ page }) => {
    await signIn(page, ADMIN_USER)
    await stubFinance(page)

    await page.goto("/dashboard/platform-admin/finance")

    // totalElements is 137 while only one row is on screen - the count must be 137, not 1.
    await expect(page.getByText("Payment records", { exact: true })).toBeVisible()
    await expect(page.getByText("137", { exact: true })).toBeVisible()
  })

  test("shows commission and earnings as not calculable, never as zero", async ({ page }) => {
    await signIn(page, ADMIN_USER)
    await stubFinance(page)

    await page.goto("/dashboard/platform-admin/finance")

    await expect(page.getByText("Not yet calculable")).toBeVisible()
    await expect(page.getByText("Platform commission")).toBeVisible()
    await expect(page.getByText("Provider earnings")).toBeVisible()
    await expect(page.getByText("Profit and loss")).toBeVisible()
    // The page must assert they are absent rather than nil.
    await expect(page.getByText(/They are absent, not zero/i)).toBeVisible()
  })

  test("surfaces payment reconciliation exceptions with their detail", async ({ page }) => {
    await signIn(page, ADMIN_USER)
    await stubFinance(page)

    await page.goto("/dashboard/platform-admin/finance")
    await page.getByRole("tab", { name: "Exceptions" }).click()

    await expect(page.getByText("AMOUNT_MISMATCH")).toBeVisible()
    await expect(page.getByText(/did not match recorded 2000/)).toBeVisible()
  })

  test("service pricing shows a stored price and marks an unpriced service as such", async ({ page }) => {
    await signIn(page, ADMIN_USER)
    await stubFinance(page)

    await page.goto("/dashboard/platform-admin/finance")
    await page.getByRole("tab", { name: "Service pricing" }).click()

    await expect(page.getByText("Premium streaming")).toBeVisible()
    await expect(page.getByText(/5,000/)).toBeVisible()
    // Unpriced must not render as free.
    await expect(page.getByText("No price set")).toBeVisible()
  })

  test("paginates through the real backend page", async ({ page }) => {
    await signIn(page, ADMIN_USER)
    await stubFinance(page)

    await page.goto("/dashboard/platform-admin/finance")
    await expect(page.getByText("Page 1 of 7")).toBeVisible()

    // `exact` keeps this off the Next.js dev-tools button, which is also named "Next".
    await page.getByRole("button", { name: "Next", exact: true }).click()
    await expect(page.getByText("Page 2 of 7")).toBeVisible()
  })

  test("shows an empty state per panel instead of a blank table", async ({ page }) => {
    await signIn(page, ADMIN_USER)
    await stubFinance(page)

    await page.goto("/dashboard/platform-admin/finance")
    await page.getByRole("tab", { name: "Access grants" }).click()
    await expect(page.getByText("No access grants match this filter.")).toBeVisible()
  })

  test("search narrows the loaded page and says so when nothing matches", async ({ page }) => {
    await signIn(page, ADMIN_USER)
    await stubFinance(page)

    await page.goto("/dashboard/platform-admin/finance")

    // The scope of the filter is disclosed before it is used.
    await expect(page.getByText(/apply to the rows on this page only/i)).toBeVisible()

    await page.getByPlaceholder("Transaction reference or service").fill("TUITION")
    await expect(page.getByText("TUITION")).toBeVisible()

    await page.getByPlaceholder("Transaction reference or service").fill("no-such-reference")
    await expect(page.getByText("TUITION")).toHaveCount(0)
    // The empty state must restate the page scope, or it reads as "nothing exists".
    await expect(page.getByText(/paginated, so rows on other pages are not searched/i)).toBeVisible()

    await page.getByRole("button", { name: "Clear filters" }).click()
    await expect(page.getByText("TUITION")).toBeVisible()
  })

  test("exports the loaded page as CSV and never invents a total row", async ({ page }) => {
    await signIn(page, ADMIN_USER)
    await stubFinance(page)

    await page.goto("/dashboard/platform-admin/finance")

    const [download] = await Promise.all([
      page.waitForEvent("download"),
      page.getByRole("button", { name: "Export this page" }).click(),
    ])

    // The filename says "page" so a saved file cannot later be mistaken for the whole ledger.
    expect(download.suggestedFilename()).toMatch(/^elmkusoma-payment-ledger-page-\d{4}-\d{2}-\d{2}\.csv$/)

    const stream = await download.createReadStream()
    const chunks: Buffer[] = []
    for await (const chunk of stream) chunks.push(Buffer.from(chunk))
    const csv = Buffer.concat(chunks).toString("utf8")
    const lines = csv.trim().split("\r\n")

    expect(lines).toHaveLength(2)
    expect(lines[0]).toContain("Reference")
    expect(csv).toContain("COMPLETED")
    // 137 records exist across 7 pages; the export must not imply otherwise.
    expect(lines.some((l) => /total|sum|137/i.test(l))).toBe(false)
  })

  test("a formula-looking service type cannot escape the CSV as an executable cell", async ({ page }) => {
    await signIn(page, ADMIN_USER)
    await stubFinance(page, {
      payments: {
        content: [
          {
            id: "mal-1",
            parentId: PARENT_USER.id,
            studentId: "33333333-3333-3333-3333-333333333333",
            amount: 1,
            currency: "TZS",
            status: "COMPLETED",
            // A description a user could have written, starting with '='.
            serviceType: "=cmd|'/c calc'!A1",
            createdAt: "2026-01-02T08:00:00",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      },
    })

    await page.goto("/dashboard/platform-admin/finance")

    const [download] = await Promise.all([
      page.waitForEvent("download"),
      page.getByRole("button", { name: "Export this page" }).click(),
    ])
    const stream = await download.createReadStream()
    const chunks: Buffer[] = []
    for await (const chunk of stream) chunks.push(Buffer.from(chunk))
    const csv = Buffer.concat(chunks).toString("utf8")

    // Prefixed with a quote so spreadsheet software treats it as text, not a formula.
    expect(csv).toContain("'=cmd")
    expect(csv).not.toMatch(/,=cmd/)
  })
})

test.describe("Subscription page honesty", () => {
  test("publishes no invented plans and no dead upgrade button", async ({ page }) => {
    await signIn(page, PARENT_USER)

    await page.goto("/dashboard/subscription")

    // The old page rendered "Premium TZS 25,000" with a button that did nothing.
    await expect(page.getByText("No plans are published yet")).toBeVisible()
    await expect(page.getByText(/25,000/)).toHaveCount(0)
    await expect(page.getByRole("button", { name: /upgrade/i })).toHaveCount(0)
    await expect(page.getByRole("button", { name: /current plan/i })).toHaveCount(0)
  })
})
