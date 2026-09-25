import type { MetadataRoute } from "next"

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL || "https://elmkusoma.co.tz"

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      // Authenticated workspaces, auth flows and internal API must not be indexed.
      disallow: ["/dashboard/", "/api/", "/login", "/register", "/forgot-password"],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  }
}
