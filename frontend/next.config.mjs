import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin("./i18n.ts");

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  typescript: {
    ignoreBuildErrors: false,
  },
  images: {
    unoptimized: true,
  },
  async rewrites() {
    const apiUrl = process.env.BACKEND_URL || "http://localhost:8080"
    const mediaUrl = process.env.MEDIA_URL || "http://localhost:8083"
    const realtimeUrl = process.env.REALTIME_URL || "http://localhost:8081"
    return [
      {
        // Only the endpoints the media service actually owns are routed to it. The Media
        // Library (list, /my, detail, search, class, recordings, create/update/delete,
        // download-url) lives on the core service at /v1/media, so the previous blanket
        // /api/v1/media/:path* rule sent requests such as /api/v1/media/my to a service
        // that has no such endpoint - and to nothing at all when it is not running.
        // Upload and presigned-upload are deliberately NOT routed here: core exposes them
        // and proxies to the media service with the caller's bearer token, so going direct
        // would bypass that authorisation.
        source: "/api/v1/media/local-content",
        destination: `${mediaUrl}/api/v1/media/local-content`,
      },
      {
        source: "/api/v1/media/health",
        destination: `${mediaUrl}/api/v1/media/health`,
      },
      {
        source: "/api/v1/media/:id/download",
        destination: `${mediaUrl}/api/v1/media/:id/download`,
      },
      {
        // The higher-education controllers are mapped at /api/v1/education/** on
        // the backend, but the generic /api/v1 rule below strips the /api prefix.
        // Keep this specific rule ahead of it so collegeApi requests (dashboard,
        // enrollments, study tasks, ...) reach the existing Spring Boot paths.
        source: "/api/v1/education/:path*",
        destination: `${apiUrl}/api/v1/education/:path*`,
      },
      {
        source: "/v1/:path*",
        destination: `${apiUrl}/v1/:path*`,
      },
      {
        source: "/api/v1/:path*",
        destination: `${apiUrl}/v1/:path*`,
      },
      {
        source: "/ws/:path*",
        destination: `${realtimeUrl}/ws/:path*`,
      },
    ]
  },
  async headers() {
    return []
  },
}

export default withNextIntl(nextConfig);
