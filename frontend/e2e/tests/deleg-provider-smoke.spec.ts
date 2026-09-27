import { test, expect } from "@playwright/test";

const BASE = "http://localhost:3000";

async function loginAsAdmin(page) {
  const res = await page.request.post("http://localhost:8080/v1/auth/login", {
    data: { email: "admin@elmkusoma.go.tz", password: "password" },
  });
  const body = await res.json();
  const token = body.data.accessToken ?? body.data.token;
  const user = {
    id: "b0000000-0000-0000-0000-000000000099",
    email: "admin@elmkusoma.go.tz",
    role: "Admin",
    firstName: "Platform",
    lastName: "Admin",
  };
  const enc = encodeURIComponent(JSON.stringify(user));
  await page.context().addCookies([
    { name: "elmkusoma_access_token", value: token, url: BASE },
    { name: "elmkusoma_current_user", value: enc, url: BASE },
    { name: "elmkusoma_institution_id", value: "a0000000-0000-0000-0000-000000000001", url: BASE },
  ]);
  await page.addInitScript(({ t, u }) => {
    localStorage.setItem("elmkusoma_access_token", t);
    localStorage.setItem("elmkusoma_current_user", u);
  }, { t: token, u: JSON.stringify(user) });
}

test("delegations workspace renders without missing i18n", async ({ page }) => {
  const missing: string[] = [];
  page.on("console", (m) => {
    if (m.type() === "error" && m.text().includes("MISSING_MESSAGE")) missing.push(m.text());
  });
  await loginAsAdmin(page);
  await page.goto(`${BASE}/dashboard/platform-admin/delegations`, { waitUntil: "networkidle" });
  await expect(page.getByRole("heading", { name: /delegation/i }).first()).toBeVisible({ timeout: 20000 });
  await expect(page.getByRole("button", { name: /create delegation/i }).first()).toBeVisible();
  expect(missing).toEqual([]);
});

test("providers registry renders without missing i18n", async ({ page }) => {
  const missing: string[] = [];
  page.on("console", (m) => {
    if (m.type() === "error" && m.text().includes("MISSING_MESSAGE")) missing.push(m.text());
  });
  await loginAsAdmin(page);
  await page.goto(`${BASE}/dashboard/platform-admin/providers`, { waitUntil: "networkidle" });
  await expect(page.getByRole("heading", { name: /provider/i }).first()).toBeVisible({ timeout: 20000 });
  expect(missing).toEqual([]);
});
