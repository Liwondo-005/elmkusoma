"use client"

import { useLocaleContext } from "@/components/locale-provider"

// Returns the global locale and re-renders live when it changes.
// Falls back to "en" outside a LocaleProvider (default context value).
export function useLocale(): string {
  return useLocaleContext().locale
}
