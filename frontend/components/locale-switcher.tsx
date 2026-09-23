"use client"

import { Globe } from "lucide-react"
import { useLocaleContext } from "@/components/locale-provider"

export function LocaleSwitcher() {
  const { locale, setLocale } = useLocaleContext()

  return (
    <div className="flex items-center gap-1.5 text-sm">
      <Globe className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
      <button
        type="button"
        onClick={() => setLocale(locale === "en" ? "sw" : "en")}
        className="rounded-md px-2 py-1 text-xs font-medium transition-colors hover:bg-accent hover:text-accent-foreground"
        aria-label="Change language"
        title={locale === "en" ? "Badilisha lugha" : "Change language"}
      >
        {locale === "en" ? "EN" : "SW"}
      </button>
    </div>
  )
}
