import { test, expect } from "@playwright/test";

const BASE = "http://localhost:3000";
const API = "http://localhost:8080/v1";

async function adminLogin(request) {
  const res = await request.post(`${API}/auth/login`, {
    data: { email: "admin@elmkusoma.go.tz", password: "password" },
  });
  const body = await res.json();
  return body.data.accessToken ?? body.data.token;
}

async function loginAs(page, email: string, password: string, role: string, id: string, institutionId?: string) {
  const res = await page.request.post(`${API}/auth/login`, { data: { email, password } });
  const body = await res.json();
  const token = body.data.accessToken ?? body.data.token;
  const user = { id, email, role, firstName: "Dele", lastName: "Gated" };
  const enc = encodeURIComponent(JSON.stringify(user));
  const cookies: any[] = [
    { name: "elmkusoma_access_token", value: token, url: BASE },
    { name: "elmkusoma_current_user", value: enc, url: BASE },
  ];
  // No institution cookie for membership-less officers: the backend IDOR guard
  // 403s any request naming an institution the caller has no membership in.
  if (institutionId) cookies.push({ name: "elmkusoma_institution_id", value: institutionId, url: BASE });
  await page.context().addCookies(cookies);
  await page.addInitScript(({ t, u }) => {
    localStorage.setItem("elmkusoma_access_token", t);
    localStorage.setItem("elmkusoma_current_user", u);
  }, { t: token, u: JSON.stringify(user) });
}

test("delegated officer approves in-scope verification in browser, denied out-of-scope via API", async ({ page, request }) => {
  test.setTimeout(240000);
  const adminTok = await adminLogin(request);
  const H = { Authorization: `Bearer ${adminTok}` };
  const missing: string[] = [];
  page.on("console", (m) => {
    if (m.type() === "error" && m.text().includes("MISSING_MESSAGE")) missing.push(m.text());
  });

  // setup: officer + NGO + verification + scoped delegation
  const email = `officer.browser.${Date.now()}@elmkusoma.go.tz`;
  const ou = await request.post(`${API}/platform-admin/users`, {
    headers: H,
    data: { firstName: "Dele", lastName: "Gated", email, password: "Password123!", role: "TEACHER" },
  });
  expect(ou.ok()).toBeTruthy();
  const officerId = (await ou.json()).data.id ?? (await ou.json()).data.userId;

  const meList = await request.get(`${API}/platform-admin/users?page=0&size=20&search=admin@elmkusoma.go.tz`, { headers: H });
  const admin = ((await meList.json()).data.content as any[]).find((u) => u.email === "admin@elmkusoma.go.tz");

  const ngo = await request.post(`${API}/platform-admin/institutions`, {
    headers: H,
    data: { name: "Browser Loop NGO", type: "NGO", email: "browser@ngo.example", country: "Tanzania" },
  });
  const ngoId = (await ngo.json()).data.id;

  const sub = await request.post(`${API}/platform-admin/verifications`, {
    headers: H,
    data: { entityType: "INSTITUTION", entityId: ngoId, verificationType: "PROVIDER_LICENSE" },
  });
  expect(sub.ok()).toBeTruthy();
  const verId = (await sub.json()).data.id;

  const deleg = await request.post(`${API}/platform-admin/delegations`, {
    headers: H,
    data: {
      delegatorId: admin.id, delegateId: officerId,
      permissions: JSON.stringify(["VIEW", "REVIEW", "VERIFY"]),
      scope: `INSTITUTION:${ngoId}`, authority: "PROVIDER_VERIFICATION",
      reason: "Browser E2E delegation",
    },
  });
  expect(deleg.ok()).toBeTruthy();
  const delegId = (await deleg.json()).data.id;

  // browser flow as the delegated officer (officers use their own workspace;
  // /dashboard/platform-admin is role-gated to Admin)
  await loginAs(page, email, "Password123!", "Teacher", officerId);
  // Dev HMR can detach the frame on first compile of a fresh route; retry navigation.
  let headed = false;
  for (let attempt = 0; attempt < 3 && !headed; attempt++) {
    try {
      await page.goto(`${BASE}/dashboard/delegated-work`, { waitUntil: "domcontentloaded" });
      await expect(page.getByRole("heading", { name: /delegated work/i }).first()).toBeVisible({ timeout: 20000 });
      headed = true;
    } catch (e) {
      if (attempt === 2) throw e;
    }
  }
  const row = page.locator("text=PROVIDER_LICENSE").first();
  await expect(row).toBeVisible({ timeout: 20000 });
  await page.getByRole("button", { name: /verify/i }).first().click();
  await expect(page.getByText(/approved/i).first()).toBeVisible({ timeout: 20000 });
  expect(missing).toEqual([]);

  // out-of-scope denial still enforced via API for the same officer
  const ngo2 = await request.post(`${API}/platform-admin/institutions`, {
    headers: H,
    data: { name: "Browser Other NGO", type: "NGO", email: "browser2@ngo.example", country: "Tanzania" },
  });
  const ngo2Id = (await ngo2.json()).data.id;
  const sub2 = await request.post(`${API}/platform-admin/verifications`, {
    headers: H,
    data: { entityType: "INSTITUTION", entityId: ngo2Id, verificationType: "PROVIDER_LICENSE" },
  });
  const ver2Id = (await sub2.json()).data.id;
  const officerRes = await page.request.post(`${API}/auth/login`, { data: { email, password: "Password123!" } });
  const officerTok = (await officerRes.json()).data.accessToken;
  const denied = await request.put(`${API}/verifications/${ver2Id}/provider-review?status=APPROVED`, {
    headers: { Authorization: `Bearer ${officerTok}` },
  });
  expect(denied.status()).toBe(403);

  // cleanup
  await request.put(`${API}/platform-admin/delegations/${delegId}/revoke?reason=e2e%20cleanup`, { headers: H });
  await request.delete(`${API}/platform-admin/institutions/${ngoId}`, { headers: H });
  await request.delete(`${API}/platform-admin/institutions/${ngo2Id}`, { headers: H });
  await request.delete(`${API}/platform-admin/users/${officerId}`, { headers: H });
});
