import type { MetadataRoute } from "next"

// Canonical site URL: NEXT_PUBLIC_SITE_URL override, else the production
// frontend domain referenced by the backend welcome-email template
// (elmkusoma-workers .../email/welcome.html → https://elmkusoma.co.tz/login).
const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL || "https://elmkusoma.co.tz"

export default function sitemap(): MetadataRoute.Sitemap {
  const now = new Date()

  // Static public routes only — dashboard workspaces are authenticated and
  // must never be indexed.
  const publicPaths = [
    "/",
    "/about",
    "/courses",
    "/contact",
    "/support",
    "/terms",
    "/privacy",
    "/login",
    "/register",
    "/forgot-password",
    "/certificates/verify",
    "/schools/nursery",
    "/schools/primary",
    "/schools/secondary",
    "/schools/colleges",
    "/schools/vocational",
    "/schools/universities",
    "/live-classes",
  ]

  return publicPaths.map((path) => ({
    url: `${SITE_URL}${path}`,
    lastModified: now,
    changeFrequency: path === "/" ? "daily" : "weekly",
    priority: path === "/" ? 1 : path.startsWith("/schools") ? 0.8 : 0.6,
  }))
}
