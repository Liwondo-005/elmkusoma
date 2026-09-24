"use client"

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react"
import type { ReactNode } from "react"
import { NextIntlClientProvider } from "next-intl"
import enMessages from "@/messages/en.json"
import swMessages from "@/messages/sw.json"

export type AppLocale = "en" | "sw"

export const SUPPORTED_LOCALES: AppLocale[] = ["en", "sw"]
export const DEFAULT_LOCALE: AppLocale = "en"
export const LOCALE_COOKIE = "NEXT_LOCALE"
export const LOCALE_STORAGE_KEY = "elmkusoma_language"

function isLocale(value: string | null | undefined): value is AppLocale {
  return value === "en" || value === "sw"
}

function readCookieLocale(): AppLocale | null {
  if (typeof document === "undefined") return null
  const match = document.cookie.match(/(?:^|;\s*)NEXT_LOCALE=([^;]*)/)
  return isLocale(match?.[1]) ? (match as RegExpMatchArray)[1] as AppLocale : null
}

function readStoredLocale(): AppLocale {
  const fromCookie = readCookieLocale()
  if (fromCookie) return fromCookie
  try {
    const stored = typeof window !== "undefined" ? window.localStorage.getItem(LOCALE_STORAGE_KEY) : null
    if (isLocale(stored)) return stored
  } catch {}
  return DEFAULT_LOCALE
}

function persistLocale(locale: AppLocale) {
  try {
    document.cookie = `${LOCALE_COOKIE}=${locale}; path=/; max-age=31536000; SameSite=Lax`
  } catch {}
  try {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, locale)
  } catch {}
  try {
    document.documentElement.setAttribute("lang", locale === "sw" ? "sw" : "en")
  } catch {}
}

// Deep-merge fallback: missing Swahili leaves fall back to English so
// t() never renders "undefined" and never crashes the app.
type Messages = Record<string, unknown>
export function mergeMessages(base: Messages, override: Messages): Messages {
  const out: Messages = { ...base }
  for (const key of Object.keys(override)) {
    const b = base[key]
    const o = override[key]
    if (
      b !== null && typeof b === "object" && !Array.isArray(b) &&
      o !== null && typeof o === "object" && !Array.isArray(o)
    ) {
      out[key] = mergeMessages(b as Messages, o as Messages)
    } else {
      out[key] = o
    }
  }
  return out
}

export function messagesForLocale(locale: AppLocale): Messages {
  if (locale === "sw") return mergeMessages(enMessages as unknown as Messages, swMessages as unknown as Messages)
  return enMessages as unknown as Messages
}

interface LocaleContextValue {
  locale: AppLocale
  setLocale: (locale: AppLocale) => void
}

const LocaleContext = createContext<LocaleContextValue>({
  locale: DEFAULT_LOCALE,
  setLocale: () => {},
})

export function LocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<AppLocale>(DEFAULT_LOCALE)

  useEffect(() => {
    const preferred = readStoredLocale()
    setLocaleState((current) => (current === preferred ? current : preferred))
    try {
      document.documentElement.setAttribute("lang", preferred === "sw" ? "sw" : "en")
    } catch {}
  }, [])

  const setLocale = useCallback((next: AppLocale) => {
    if (!isLocale(next)) return
    persistLocale(next)
    setLocaleState(next)
  }, [])

  const value = useMemo(() => ({ locale, setLocale }), [locale, setLocale])
  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>
}

export function useLocaleContext(): LocaleContextValue {
  return useContext(LocaleContext)
}

// Wraps any subtree with the correctly-resolved next-intl messages.
// Mounted once in app/providers.tsx so translations work on EVERY page,
// including public and authentication pages outside /dashboard.
export function LocaleMessages({ children }: { children: ReactNode }) {
  const { locale } = useLocaleContext()
  const messages = useMemo(() => messagesForLocale(locale), [locale])
  return (
    <NextIntlClientProvider locale={locale} messages={messages} timeZone="Africa/Dar_es_Salaam">
      {children}
    </NextIntlClientProvider>
  )
}
