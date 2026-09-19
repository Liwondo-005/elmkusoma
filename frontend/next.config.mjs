/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  typescript: {
    ignoreBuildErrors: true,
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
        source: "/api/v1/media/:path*",
        destination: `${mediaUrl}/api/v1/media/:path*`,
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

export default nextConfig;
