"use client"

import { useEffect, useState } from "react"

const DEFAULT_LOCALE = "en"

export function useLocale(): string {
  const [locale, setLocale] = useState(DEFAULT_LOCALE)

  useEffect(() => {
    const match = document.cookie.match(/(?:^|;\s*)NEXT_LOCALE=([^;]*)/)
    if (match) {
      setLocale(match[1])
    }
  }, [])

  return locale
}
