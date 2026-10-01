"use client"

import { useTranslations } from "next-intl";

import { useCallback, useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { FlaskConical, RefreshCw, AlertCircle, CheckCircle2 } from "lucide-react"
import { oversightApi, type OversightDataQuality } from "@/lib/api"
import { cn } from "@/lib/utils"

export default function OversightDataQualityPage() {
  const t = useTranslations("oversight")
  const { user, loading: authLoading } = useRequireAuth()
  const [data, setData] = useState<OversightDataQuality | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setData(await oversightApi.dataQuality())
    } catch {
      setError(t("dataQuality.loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    if (authLoading || !user) return
    load()
  }, [authLoading, user, load])

  if (authLoading || loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">{t("dataQuality.loading")}</div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
              <FlaskConical className="size-5 text-primary" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-foreground">{t("dataQuality.title")}</h1>
              <p className="text-sm text-muted-foreground">{t("dataQuality.subtitle")}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={load}
            className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <RefreshCw className="size-4" />
            {t("dataQuality.refresh")}
          </button>
        </div>

        {data && (
          <div className="mt-4 flex flex-wrap items-center gap-4 border-t border-border pt-4">
            <div className="flex items-center gap-2">
              <span className="text-3xl font-bold text-foreground">{data.score}%</span>
              <span className="text-sm text-muted-foreground">{t("dataQuality.score")}</span>
            </div>
            <div
              className={cn(
                "rounded-full px-3 py-1 text-xs font-medium",
                data.totalIssues === 0 ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700",
              )}
            >
              {data.totalIssues} {t("dataQuality.affectedRecords")}
            </div>
          </div>
        )}
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
          <AlertCircle className="size-4 shrink-0" />
          {error}
        </div>
      )}

      {data && data.totalIssues === 0 && (
        <div className="flex items-center gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-6">
          <CheckCircle2 className="size-5 shrink-0 text-emerald-600" />
          <p className="text-sm font-medium text-emerald-800">{t("dataQuality.noIssues")}</p>
        </div>
      )}

      {data && (
        <div className="space-y-4">
          {data.checks.map((check) => (
            <div key={check.id} className="rounded-2xl border border-border bg-card p-5">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h2 className="font-semibold text-foreground">{check.title}</h2>
                  <p className="mt-0.5 text-sm text-muted-foreground">{check.description}</p>
                </div>
                <div className="flex items-center gap-2">
                  <span
                    className={cn(
                      "inline-flex items-center rounded-full px-2 py-1 text-xs font-medium",
                      check.severity === "HIGH"
                        ? "bg-red-100 text-red-700"
                        : check.severity === "MEDIUM"
                          ? "bg-orange-100 text-orange-700"
                          : "bg-yellow-100 text-yellow-700",
                    )}
                  >
                    {check.severity}
                  </span>
                  <span
                    className={cn(
                      "inline-flex items-center rounded-full px-2 py-1 text-xs font-bold",
                      check.affectedCount === 0
                        ? "bg-emerald-100 text-emerald-700"
                        : "bg-red-100 text-red-700",
                    )}
                  >
                    {check.affectedCount}
                  </span>
                </div>
              </div>
              {check.affectedCount > 0 && check.sample.length > 0 && (
                <ul className="mt-3 flex flex-wrap gap-2">
                  {check.sample.map((entry) => (
                    <li
                      key={entry}
                      className="rounded-md bg-muted px-2 py-1 text-xs text-muted-foreground"
                    >
                      {entry}
                    </li>
                  ))}
                  {check.affectedCount > check.sample.length && (
                    <li className="rounded-md bg-muted px-2 py-1 text-xs text-muted-foreground">
                      +{check.affectedCount - check.sample.length}
                    </li>
                  )}
                </ul>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
