import { readFileSync } from "node:fs"
import { join } from "node:path"

/**
 * GET /sw.js — the elmkusoma service worker, served from the app router so
 * its cache generation tracks the build.
 *
 * Gap H (service worker versioning): the worker used to live in /public with
 * a hard-coded `elmkusoma-v1` cache name. Its bytes never changed between
 * deploys, so browsers kept the old worker registered and its stale `_next`
 * chunk copies served indefinitely (old-hash chunks 404ing after a deploy).
 * Serving it from a route handler injects the production BUILD_ID into the
 * cache name: every build produces different script bytes (worker updates) and
 * a fresh cache generation, and the activate handler's `elmkusoma-` prefix
 * sweep drops all previous generations.
 *
 * `force-dynamic` evaluates this handler per request, so the BUILD_ID is read
 * at runtime — correct in the standalone output (which ships .next/BUILD_ID)
 * with no dependence on build-step ordering. Registration stays at /sw.js
 * (unchanged) in app/dashboard/learner/layout.tsx.
 */

export const dynamic = "force-dynamic"
export const runtime = "nodejs"

function resolveCacheName(): string {
  if (process.env.NODE_ENV !== "production") {
    // Dev chunks change on every edit — scope the cache to this dev process
    // so each dev session starts from a clean generation.
    return `elmkusoma-dev-${process.pid}`
  }
  try {
    const buildId = readFileSync(join(process.cwd(), ".next", "BUILD_ID"), "utf8").trim()
    if (buildId.length > 0) return `elmkusoma-${buildId}`
  } catch {
    // No BUILD_ID visible (odd layout) — fall through to the safe default.
  }
  return `elmkusoma-dev-${process.pid}`
}

function serviceWorkerSource(cacheName: string): string {
  // String.raw keeps the regex escapes below byte-identical to the original
  // worker; the only interpolation is the cache generation name.
  return String.raw`/* elmkusoma service worker — cache generation: ${cacheName} */
const CACHE_NAME = "${cacheName}"

self.addEventListener("install", (event) => {
  self.skipWaiting()
  event.waitUntil(
    caches.open(CACHE_NAME).catch(() => {})
  )
})

self.addEventListener("activate", (event) => {
  event.waitUntil(
    (async () => {
      const keys = await caches.keys()
      await Promise.all(
        keys
          .filter((key) => key.startsWith("elmkusoma-") && key !== CACHE_NAME)
          .map((key) => caches.delete(key))
      )
      await self.clients.claim()
    })()
  )
})

self.addEventListener("fetch", (event) => {
  const request = event.request

  if (request.method !== "GET") return

  const url = new URL(request.url)
  if (url.protocol !== "http:" && url.protocol !== "https:") return

  const isNavigation = request.mode === "navigate"
  const isSameOrigin = url.origin === self.location.origin
  const isStaticAsset =
    isSameOrigin &&
    (url.pathname.startsWith("/_next/static/") ||
      url.pathname.startsWith("/_next/image") ||
      url.pathname.startsWith("/images/") ||
      /\.(?:png|jpe?g|gif|svg|webp|avif|ico|css|js|woff2?)$/.test(url.pathname))

  if (isNavigation) {
    event.respondWith(
      (async () => {
        try {
          const response = await fetch(request)
          if (response && response.ok) {
            const cache = await caches.open(CACHE_NAME)
            cache.put(request, response.clone())
          }
          return response
        } catch {
          const cached = await caches.match(request)
          if (cached) return cached
          return new Response("You are offline. Please reconnect and try again.", {
            status: 503,
            statusText: "Offline",
            headers: { "Content-Type": "text/plain; charset=utf-8" }
          })
        }
      })()
    )
    return
  }

  if (isStaticAsset) {
    event.respondWith(
      (async () => {
        const cached = await caches.match(request)
        if (cached) return cached
        try {
          const response = await fetch(request)
          if (response && response.ok) {
            const cache = await caches.open(CACHE_NAME)
            cache.put(request, response.clone())
          }
          return response
        } catch {
          return cached || Response.error()
        }
      })()
    )
  }
})
`
}

export function GET(): Response {
  return new Response(serviceWorkerSource(resolveCacheName()), {
    headers: {
      "Content-Type": "text/javascript; charset=utf-8",
      "Cache-Control": "no-cache, must-revalidate, max-age=0",
    },
  })
}
