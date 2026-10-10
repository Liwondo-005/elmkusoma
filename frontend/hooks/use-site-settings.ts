"use client"

import { useEffect, useState } from "react"
import { publicSiteApi, type SiteSettings } from "@/lib/public-site-api"

/**
 * Shared public site settings.
 *
 * The footer, landing contact section, contact page and support page all need the same handful
 * of values. Fetching it per component would mean four round trips and, worse, four chances for
 * them to disagree on screen. This caches one in-flight promise per page load.
 *
 * The cache is module-level and therefore process-global, so it is only consulted in the browser.
 * On the server it always re-fetches, which keeps one visitor's settings out of another
 * visitor's server render.
 */

const EMPTY: SiteSettings = {}

let cached: SiteSettings | null = null
let inFlight: Promise<SiteSettings> | null = null

function isBrowser() {
  return typeof window !== "undefined"
}

async function load(): Promise<SiteSettings> {
  if (cached) return cached
  if (!inFlight) {
    inFlight = publicSiteApi.getSettings().then((settings) => {
      cached = settings
      return settings
    })
  }
  return inFlight
}

/** Test seam: drops the cache so a test can assert against a fresh load. */
export function resetSiteSettingsCache() {
  cached = null
  inFlight = null
}

export interface UseSiteSettingsResult {
  settings: SiteSettings
  loading: boolean
  /** Resolves a configured value, or undefined when it was never set. */
  value: (key: keyof SiteSettings) => string | undefined
}

export function useSiteSettings(): UseSiteSettingsResult {
  const [settings, setSettings] = useState<SiteSettings>(EMPTY)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    load()
      .then((loaded) => {
        if (active) setSettings(loaded)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  return {
    settings,
    loading,
    value: (key) => {
      const raw = settings[key]
      if (!raw) return undefined
      const trimmed = raw.trim()
      return trimmed.length > 0 ? trimmed : undefined
    },
  }
}

/**
 * Formats a configured WhatsApp number as a click-to-chat link.
 *
 * A click-to-chat link opens a chat the visitor sends themselves; it does not deliver anything
 * to the support team. Callers must not present it as automated messaging.
 */
export function whatsappClickToChat(digits?: string, message?: string): string | undefined {
  if (!digits) return undefined
  const cleaned = digits.replace(/\D/g, "")
  if (cleaned.length < 8) return undefined
  const base = `https://wa.me/${cleaned}`
  return message ? `${base}?text=${encodeURIComponent(message)}` : base
}

/** Formats a configured number as a tel: href, stripping formatting characters. */
export function telHref(phone?: string): string | undefined {
  if (!phone) return undefined
  const cleaned = phone.replace(/[^\d+]/g, "")
  return cleaned.length >= 7 ? `tel:${cleaned}` : undefined
}