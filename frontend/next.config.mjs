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
        source: "/v1/:path*",
        destination: "http://localhost:8080/v1/:path*",
      },
      {
        source: "/v1/:path*",
        destination: "http://localhost:8080/v1/:path*",
      },
    ]
  },
}

export default nextConfig;
