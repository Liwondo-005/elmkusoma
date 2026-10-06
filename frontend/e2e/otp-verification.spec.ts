import { test, expect, type Page } from "@playwright/test"
import { execFileSync, spawn } from "node:child_process"
import { readFileSync } from "node:fs"
import os from "node:os"
import path from "node:path"

// Production OTP verification experience (PROMPT §1-46): the /verify-otp UI on
// top of the pre-existing /v1/auth/send-code + /v1/auth/verify-code contract.
// The real code is read from `verification_codes` — never guessed, never mocked.

const CODE_LENGTH = 5
const INVALID_CODE_MESSAGE =
  "Invalid verification code. Please check the code and try again, or request a new one."

function freshEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@test.com`
}

function readDbEnv(): Record<string, string> {
  const raw = readFileSync(path.resolve(__dirname, "../../backend/.env"), "utf8")
  const env: Record<string, string> = {}
  for (const line of raw.split("\n")) {
    const match = line.match(/^([A-Z_]+)=(.*)$/)
    if (match) env[match[1]] = match[2].trim()
  }
  return env
}

function fetchOtpCode(email: string): string {
  const db = readDbEnv()
  const sql =
    "SELECT code FROM verification_codes WHERE lower(email) = lower('" +
    email +
    "') AND used = false ORDER BY created_at DESC LIMIT 1"
  const out = execFileSync(
    "psql",
    [
      "-h",
      db.DB_HOST || "localhost",
      "-p",
      db.DB_PORT || "5432",
      "-U",
      db.DB_USERNAME,
      "-d",
      db.DB_NAME,
      "-tAc",
      sql,
    ],
    { env: { ...process.env, PGPASSWORD: db.DB_PASSWORD } },
  ).toString().trim()
  expect(out, "a live unused code must exist in verification_codes").toMatch(/^\d{5}$/)
  return out
}

function otpGroup(page: Page) {
  return page.getByRole("group", { name: /Verification code/ })
}

function otpTiles(page: Page) {
  return otpGroup(page).getByRole("textbox")
}

async function waitForCodeSent(page: Page) {
  // The countdown only appears after the backend confirmed the send.
  await expect(page.getByText(/Resend in \d{2}:\d{2}/)).toBeVisible({ timeout: 30000 })
}

async function fillCode(page: Page, code: string) {
  expect(code).toHaveLength(CODE_LENGTH)
  for (let i = 0; i < CODE_LENGTH; i += 1) {
    await otpTiles(page).nth(i).fill(code[i])
  }
}

test("auto-send, real code verification and redirect to login", async ({ page }) => {
  const email = freshEmail("otp-e2e-verify")
  await page.goto(`/verify-otp?email=${encodeURIComponent(email)}`)

  await expect(page.getByRole("heading", { name: "Verify OTP" })).toBeVisible()
  await waitForCodeSent(page)

  const code = fetchOtpCode(email)
  await fillCode(page, code)

  const verifyButton = page.getByRole("button", { name: "Verify Code" })
  await expect(verifyButton).toBeEnabled()
  await verifyButton.click()

  await expect(page.getByRole("button", { name: "OTP Verified" })).toBeVisible({ timeout: 15000 })
  await page.waitForURL("**/login", { timeout: 15000 })
})

test("wrong code is rejected safely with an actionable message", async ({ page }) => {
  const email = freshEmail("otp-e2e-wrong")
  await page.goto(`/verify-otp?email=${encodeURIComponent(email)}`)
  await waitForCodeSent(page)

  const real = fetchOtpCode(email)
  const wrong = (real[0] === "0" ? "1" : "0") + real.slice(1)
  await fillCode(page, wrong)

  await page.getByRole("button", { name: "Verify Code" }).click()

  // Safe, generic, actionable feedback — no code echo, no stack traces.
  await expect(page.getByText(INVALID_CODE_MESSAGE)).toBeVisible({ timeout: 15000 })
  await expect(page).toHaveURL(/\/verify-otp/)
  // Still actionable: the button is back to idle and the field stays editable.
  await expect(page.getByRole("button", { name: "Verify Code" })).toBeEnabled()
})

test("verify stays disabled until all 5 digits are present", async ({ page }) => {
  const email = freshEmail("otp-e2e-format")
  await page.goto(`/verify-otp?email=${encodeURIComponent(email)}`)
  await waitForCodeSent(page)

  const verifyButton = page.getByRole("button", { name: "Verify Code" })
  await expect(verifyButton).toBeDisabled()

  for (let i = 0; i < CODE_LENGTH - 1; i += 1) {
    await otpTiles(page).nth(i).fill(String((i + 1) % 10))
  }
  await expect(verifyButton).toBeDisabled()

  await otpTiles(page).nth(CODE_LENGTH - 1).fill("7")
  await expect(verifyButton).toBeEnabled()
})

test("reload during the cooldown resumes waiting without claiming a new send", async ({
  page,
}) => {
  const email = freshEmail("otp-e2e-cooldown")
  await page.goto(`/verify-otp?email=${encodeURIComponent(email)}`)
  await waitForCodeSent(page)
  const code = fetchOtpCode(email)

  // Back within the 60s window: the auto-send hits the backend cooldown and
  // the UI must keep waiting honestly instead of pretending a new code exists.
  await page.reload()
  await expect(page.getByText(/A code was sent recently/)).toBeVisible({ timeout: 30000 })
  await waitForCodeSent(page)

  // The original live code still verifies — code state lives server-side.
  await fillCode(page, code)
  await page.getByRole("button", { name: "Verify Code" }).click()
  await expect(page.getByRole("button", { name: "OTP Verified" })).toBeVisible({ timeout: 15000 })
})

test("forgot-password recovery screen offers verify-with-a-code", async ({ page }) => {
  const email = freshEmail("otp-e2e-recovery")
  await page.goto("/forgot-password")
  await page.locator("#email").fill(email)
  await page.locator('button[type="submit"]').click()

  await expect(
    page.getByRole("link", { name: /Verify with a code instead/ }),
  ).toBeVisible({ timeout: 15000 })

  await page
    .getByRole("link", { name: /Verify with a code instead/ })
    .click()

  await page.waitForURL(/\/verify-otp\?email=/, { timeout: 15000 })
  await expect(page.getByRole("heading", { name: "Verify OTP" })).toBeVisible()
  // Deep link pre-fills the masked destination and auto-requests the code.
  await waitForCodeSent(page)
  await expect(page.getByText(/Code sent to /)).toBeVisible()
})

// Requires the backend with default spring.mail (localhost:1025) — the sink
// below impersonates that SMTP endpoint and records the real transmission.
test("the issued code is actually delivered over SMTP", async ({ page }) => {
  const outFile = path.join(os.tmpdir(), `smtp-sink-${Date.now()}.json`)
  const sink = spawn(
    "node",
    [path.resolve(__dirname, "tools/smtp-sink.mjs"), "1025", outFile],
    { stdio: "ignore" },
  )
  try {
    const email = freshEmail("otp-e2e-mail")
    await page.goto(`/verify-otp?email=${encodeURIComponent(email)}`)
    await waitForCodeSent(page)
    const code = fetchOtpCode(email)

    const messages = JSON.parse(readFileSync(outFile, "utf8"))
    const message = messages.find((m: { to: string[] }) =>
      m.to.some((addr) => addr.toLowerCase() === email.toLowerCase()),
    )
    expect(message, "the sink must receive one message for the requester").toBeTruthy()
    expect(message.subject).toBe("Your ELMKUSOMA verification code")
    expect(message.body).toContain(code)
    expect(message.body).toContain("10 minutes")
  } finally {
    sink.kill()
  }
})

// §33 narrow mobile: fit the viewport, no horizontal scroll, tiles touch-sized.
test("fits a narrow mobile viewport without horizontal scrolling", async ({ browser }) => {
  const context = await browser.newContext({
    viewport: { width: 320, height: 640 },
    isMobile: true,
    hasTouch: true,
  })
  const page = await context.newPage()
  try {
    const email = freshEmail("otp-e2e-mobile")
    await page.goto(`/verify-otp?email=${encodeURIComponent(email)}`)
    await waitForCodeSent(page)

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - window.innerWidth,
    )
    expect(overflow, "page must not scroll horizontally at 320px").toBeLessThanOrEqual(0)

    const tiles = otpTiles(page)
    await expect(tiles).toHaveCount(CODE_LENGTH)
    for (let i = 0; i < CODE_LENGTH; i += 1) {
      const box = await tiles.nth(i).boundingBox()
      expect(box, "tile must be rendered").toBeTruthy()
      expect(box!.width, "touch-friendly tile width").toBeGreaterThanOrEqual(40)
      expect(box!.x + box!.width, "tile must stay inside the viewport").toBeLessThanOrEqual(320)
    }
    await expect(page.getByRole("button", { name: "Verify Code" })).toBeVisible()
  } finally {
    await context.close()
  }
})
