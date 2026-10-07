"use client";

import { useEffect } from "react";

/**
 * Service worker lifecycle for the app origin.
 *
 * Production: registers /sw.js (app/sw.js/route.ts), whose cache generation is
 * the BUILD_ID, so each deploy invalidates the previous generation.
 *
 * Development: registers nothing and actively tears down anything left over.
 * A worker that caches dev bundles is a footgun - Turbopack rewrites chunk
 * hashes on every edit, so an installed worker happily serves the previous
 * `_next/static` copy and the browser runs code that no longer exists on disk.
 * That is exactly how a fixed page kept crashing with a stale
 * "certs.map is not a function" after the dev server had already been
 * restarted and the source corrected. Dev must therefore always talk to the
 * server, and must clean up any worker/cache a previous session installed.
 */
export function ServiceWorkerRegistrar() {
  useEffect(() => {
    if (typeof navigator === "undefined" || !("serviceWorker" in navigator)) return;

    if (process.env.NODE_ENV === "production") {
      navigator.serviceWorker.register("/sw.js").catch(() => {});
      return;
    }

    // Dev: unregister every worker on this origin and drop the app's caches so
    // the next load is served by the dev server itself.
    let cancelled = false;
    (async () => {
      try {
        const registrations = await navigator.serviceWorker.getRegistrations();
        await Promise.all(registrations.map((registration) => registration.unregister()));
      } catch {
        /* nothing to clean up */
      }
      try {
        if (typeof caches !== "undefined") {
          const keys = await caches.keys();
          await Promise.all(
            keys
              .filter((key) => key.startsWith("elmkusoma-"))
              .map((key) => caches.delete(key)),
          );
        }
      } catch {
        /* cache storage unavailable */
      }
      if (!cancelled && "controller" in navigator.serviceWorker) {
        // An already-installed worker keeps controlling this page until it is
        // released; without this the old cached chunks can still answer the
        // very next navigation.
        await navigator.serviceWorker
          .getRegistrations()
          .then((rs) => Promise.all(rs.map((r) => r.unregister())))
          .catch(() => {});
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  return null;
}