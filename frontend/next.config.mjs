/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },
  async rewrites() {
    return [
      {
        source: "/api/v1/:path*",
        destination: "http://localhost:8080/v1/:path*",
      },
      {
        source: "/v1/:path*",
        destination: "http://localhost:8080/v1/:path*",
      },
      {
        source: "/api/v1/media/:path*",
        destination: "http://localhost:8083/api/v1/media/:path*",
      },
      {
        source: "/ws/:path*",
        destination: "http://localhost:8081/ws/:path*",
      },
    ]
  },
  // Explicitly bind to all interfaces
  async headers() {
    return []
  },
}

export default nextConfig;
