import { test, expect } from "@playwright/test";

const BASE = "http://localhost:3000";
const API = "http://localhost:8080";

async function apiLogin(page, email, password) {
  const res = await page.request.post(`${API}/v1/auth/login`, {
    data: { email, password },
  });
  const body = await res.json();
  expect(body.success).toBeTruthy();
  const token = body.data.accessToken;
  const user = body.data.user;
  const roleMap = {
    STUDENT: "Student",
    TEACHER: "Teacher",
    PARENT: "Parent",
    OTHER_LEARNER: "Other Learner",
    ADMIN: "Admin",
    INSTITUTION_ADMIN: "Institution Admin",
  };
  user.role = roleMap[user.role] ?? user.role;
  const enc = encodeURIComponent(JSON.stringify(user));
  await page.context().addCookies([
    { name: "elmkusoma_access_token", value: token, url: BASE },
    { name: "elmkusoma_current_user", value: enc, url: BASE },
    { name: "elmkusoma_institution_id", value: user.institutionId ?? "", url: BASE },
  ]);
  await page.addInitScript(
    ({ t, u, i }) => {
      localStorage.setItem("elmkusoma_access_token", t);
      localStorage.setItem("elmkusoma_current_user", u);
      if (i) localStorage.setItem("elmkusoma_institution_id", i);
    },
    { t: token, u: JSON.stringify(user), i: user.institutionId ?? "" }
  );
  return { token, user };
}

function trackMissing(page): string[] {
  const missing: string[] = [];
  page.on("console", (m) => {
    if (m.type() === "error" && m.text().includes("MISSING_MESSAGE")) missing.push(m.text());
  });
  return missing;
}

async function ensureOtherLearner(page) {
  const email = "e2e-learner@elmkusoma.test";
  const password = "Test123!";
  const loginRes = await page.request.post(`${API}/v1/auth/login`, {
    data: { email, password },
  });
  if (!loginRes.ok()) {
    await page.request.post(`${API}/v1/auth/register`, {
      data: {
        firstName: "E2E",
        lastName: "Learner",
        email,
        password,
        role: "OTHER_LEARNER",
      },
    });
  }
  return apiLogin(page, email, password);
}

test("teacher can create and delete a learning offering", async ({ page }) => {
  test.setTimeout(120000);
  const missing = trackMissing(page);
  await apiLogin(page, "teacher1@darms.edu.tz", "password");
  page.on("dialog", (d) => d.accept());

  await page.goto(`${BASE}/dashboard/teacher/learning-offerings`, { waitUntil: "domcontentloaded" });
  await expect(page.getByRole("heading", { name: "Learning Offerings", level: 1 }).first()).toBeVisible({ timeout: 30000 });

  const title = `E2E Smoke Offering ${Date.now()}`;
  await page.getByRole("button", { name: "New Learning Offering" }).click();
  await page.getByPlaceholder("e.g. Form Four Mathematics Exam Preparation").fill(title);
  await page.getByRole("button", { name: "Create Offering" }).click();
  await expect(page.getByText("Learning offering created")).toBeVisible({ timeout: 15000 });
  await expect(page.getByRole("heading", { name: title })).toBeVisible({ timeout: 15000 });

  const card = page.locator("div.rounded-2xl.border", { hasText: title });
  await card.getByRole("button", { name: "Delete" }).click();
  await expect(page.getByText("Learning offering deleted")).toBeVisible({ timeout: 15000 });
  await expect(page.getByRole("heading", { name: title })).toHaveCount(0, { timeout: 15000 });

  expect(missing).toEqual([]);
});

test("learner explore tab shows offerings and detail page renders", async ({ page }) => {
  test.setTimeout(120000);
  const missing = trackMissing(page);
  const t = await apiLogin(page, "teacher1@darms.edu.tz", "password");
  const s = await ensureOtherLearner(page);
  expect(s.user.role).toBe("Other Learner");

  const title = `E2E Detail Offering ${Date.now()}`;
  const createRes = await page.request.post(`${API}/v1/teachers/me/offerings`, {
    headers: {
      Authorization: `Bearer ${t.token}`,
      "X-Institution-Id": t.user.institutionId,
      "Content-Type": "application/json",
    },
    data: {
      title,
      description: "Created by E2E smoke test",
      educationLevel: "SECONDARY",
      visibility: "PUBLIC",
      status: "PUBLISHED",
      independent: true,
    },
  });
  expect(createRes.ok()).toBeTruthy();
  const created = (await createRes.json()).data;
  expect(created.id).toBeTruthy();

  try {
    await page.goto(`${BASE}/dashboard/learner/courses`, { waitUntil: "domcontentloaded" });
    await page.getByRole("tab", { name: "Learning Offerings" }).click();
    const cardLink = page.getByRole("link", { name: new RegExp(title) });
    await expect(cardLink).toBeVisible({ timeout: 20000 });
    await expect(page.getByText(/By /).first()).toBeVisible();

    await page.goto(`${BASE}/dashboard/learner/offerings/${created.id}`, { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { name: title, level: 1 })).toBeVisible({ timeout: 30000 });
    await expect(page.getByText("Back to Explore")).toBeVisible();
    await expect(page.getByText("About this offering")).toBeVisible();
    await expect(page.getByText("Created by E2E smoke test")).toBeVisible();
    expect(missing).toEqual([]);
  } finally {
    await page.request.delete(`${API}/v1/teachers/me/offerings/${created.id}`, {
      headers: {
        Authorization: `Bearer ${t.token}`,
        "X-Institution-Id": t.user.institutionId,
      },
    }).catch(() => {});
  }
});

test("draft offerings are hidden from learners", async ({ page }) => {
  test.setTimeout(120000);
  const t = await apiLogin(page, "teacher1@darms.edu.tz", "password");
  await ensureOtherLearner(page);

  const createRes = await page.request.post(`${API}/v1/teachers/me/offerings`, {
    headers: {
      Authorization: `Bearer ${t.token}`,
      "X-Institution-Id": t.user.institutionId,
      "Content-Type": "application/json",
    },
    data: {
      title: `E2E Draft ${Date.now()}`,
      status: "DRAFT",
      visibility: "PUBLIC",
      independent: true,
    },
  });
  expect(createRes.ok()).toBeTruthy();
  const draft = (await createRes.json()).data;

  try {
    await page.goto(`${BASE}/dashboard/learner/offerings/${draft.id}`, { waitUntil: "domcontentloaded" });
    await expect(page.getByText("This learning offering is not available.")).toBeVisible({ timeout: 30000 });
  } finally {
    await page.request.delete(`${API}/v1/teachers/me/offerings/${draft.id}`, {
      headers: {
        Authorization: `Bearer ${t.token}`,
        "X-Institution-Id": t.user.institutionId,
      },
    }).catch(() => {});
  }
});


