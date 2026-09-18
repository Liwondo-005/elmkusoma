"use client"

import { Globe } from "lucide-react"
import { useLocale } from "@/hooks/use-locale"

export function LocaleSwitcher() {
  const locale = useLocale()

  function switchLocale(newLocale: string) {
    document.cookie = `NEXT_LOCALE=${newLocale}; path=/; max-age=31536000; SameSite=Lax`
    window.location.reload()
  }

  return (
    <div className="flex items-center gap-1.5 text-sm">
      <Globe className="h-4 w-4 text-muted-foreground" />
      <button
        onClick={() => switchLocale(locale === "en" ? "sw" : "en")}
        className="rounded-md px-2 py-1 text-xs font-medium transition-colors hover:bg-accent hover:text-accent-foreground"
      >
        {locale === "en" ? "Kiswahili" : "English"}
      </button>
    </div>
  )
}
