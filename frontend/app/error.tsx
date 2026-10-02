"use client"

import Link from "next/link"
import { useEffect } from "react"
import { useTranslations } from "next-intl"
import { AlertTriangle, Home, RefreshCw } from "lucide-react"

// Next 16 error boundary: receives { error, retry } (not the older `reset`).
export default function Error({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  const t = useTranslations("common")
  useEffect(() => {
    // Surface the real error in dev tooling; never render its message to users.
    console.error(error)
  }, [error])

  return (
    <div className="flex min-h-[60vh] items-center justify-center px-4">
      <div className="w-full max-w-md rounded-2xl border border-border bg-card p-8 text-center shadow-xs">
        <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-destructive/10">
          <AlertTriangle className="size-7 text-destructive" />
        </div>
        <h2 className="mt-4 text-xl font-bold text-foreground">{t("error.generic")}</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          {t("error.pageLoad")}
        </p>
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          <button
            onClick={() => retry()}
            className="inline-flex h-10 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            <RefreshCw className="size-4" /> {t("retry")}
          </button>
          <Link
            href="/"
            className="inline-flex h-10 items-center gap-2 rounded-lg border border-border px-4 text-sm font-medium text-foreground hover:bg-muted"
          >
            <Home className="size-4" /> {t("error.homepage")}
          </Link>
        </div>
        {error.digest && (
          <p className="mt-4 text-[10px] text-muted-foreground">{t("error.reference", { digest: error.digest })}</p>
        )}
      </div>
    </div>
  )
}
