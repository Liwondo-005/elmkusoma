"use client"

import { useCallback, useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import {
  AlertCircle,
  CheckCircle2,
  Loader2,
  Puzzle,
  RefreshCw,
  Settings,
} from "lucide-react"
import {
  adminApi,
  getInstitutionId,
  type InstitutionServiceItem,
} from "@/lib/api"
import { useToast } from "@/components/toast"
import { cn } from "@/lib/utils"

interface ServiceRow {
  key: string
  name: string
  description: string
  enabled: boolean
  enabledAt: string | null
}

export default function AdminServicesPage() {
  const t = useTranslations("admin")
  const tc = useTranslations("common")
  const { toast } = useToast()

  const [rows, setRows] = useState<ServiceRow[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [recordsNotice, setRecordsNotice] = useState<string | null>(null)
  const [savingKey, setSavingKey] = useState<string | null>(null)

  const load = useCallback(() => {
    const institutionId = getInstitutionId()
    if (!institutionId) {
      setError(t("services.noInstitutionContextFound"))
      setLoading(false)
      return
    }

    setLoading(true)
    setError(null)
    setRecordsNotice(null)

    const profileRequest = adminApi.getOrgProfile(institutionId)
    const recordsRequest = adminApi
      .getInstitutionServices()
      .catch(() => {
        setRecordsNotice(t("services.recordsUnavailable"))
        return null
      })

    Promise.all([profileRequest, recordsRequest])
      .then(([profile, records]) => {
        const enabledList = (profile.enabledServices || [])
          .map((s) => s.trim())
          .filter(Boolean)
        const recordMap = new Map<string, InstitutionServiceItem>()
        for (const record of records ?? []) {
          if (record?.featureKey) recordMap.set(record.featureKey, record)
        }

        const keys = new Set<string>([
          ...recordMap.keys(),
          ...enabledList,
        ])

        const next: ServiceRow[] = [...keys]
          .sort((a, b) => a.localeCompare(b))
          .map((key) => {
            const record = recordMap.get(key)
            const enabled = enabledList.includes(key)
            return {
              key,
              name: record?.featureName?.trim() || key,
              description: record?.description?.trim() || "",
              enabled,
              enabledAt: enabled && record?.enabledAt ? record.enabledAt : null,
            }
          })

        setRows(next)
      })
      .catch((err) =>
        setError(err instanceof Error ? err.message : t("services.failedToLoad")),
      )
      .finally(() => setLoading(false))
  }, [t])

  useEffect(() => {
    load()
  }, [load])

  async function toggleService(row: ServiceRow) {
    if (savingKey) return
    const institutionId = getInstitutionId()
    if (!institutionId) return

    const desired = row.enabled
      ? rows.filter((r) => r.key !== row.key).map((r) => r.key)
      : [...rows.filter((r) => r.enabled).map((r) => r.key), row.key]

    setSavingKey(row.key)
    const previous = rows
    setRows((prev) =>
      prev.map((r) => (r.key === row.key ? { ...r, enabled: !r.enabled } : r)),
    )

    try {
      await adminApi.updateEnabledServices(institutionId, desired)
      toast(
        row.enabled
          ? t("services.disabledToast", { name: row.name })
          : t("services.enabledToast", { name: row.name }),
        "success",
      )
      load()
    } catch (err) {
      setRows(previous)
      toast(
        err instanceof Error && err.message ? err.message : t("services.failedToSave"),
        "error",
      )
    } finally {
      setSavingKey(null)
    }
  }

  const enabledCount = rows.filter((r) => r.enabled).length

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("services.title")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("services.subtitle")}</p>
        </div>
        {!loading && !error && (
          <span className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-muted/60 px-3 py-1.5 text-xs font-medium text-muted-foreground">
            <CheckCircle2 className="size-3.5 text-emerald-500" />
            {t("services.enabledOf", { enabled: enabledCount, total: rows.length })}
          </span>
        )}
      </div>

      {recordsNotice && (
        <div className="flex items-start gap-2 rounded-2xl border border-border bg-muted/40 px-4 py-3 text-xs text-muted-foreground">
          <AlertCircle className="mt-0.5 size-3.5 shrink-0" />
          <p>{recordsNotice}</p>
        </div>
      )}

      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {error && !loading && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
          <p className="text-sm font-medium text-destructive">{error}</p>
          <button
            type="button"
            onClick={load}
            className="mt-4 inline-flex h-9 items-center gap-2 rounded-lg border border-destructive/30 px-4 text-sm font-medium text-destructive hover:bg-destructive/10"
          >
            <RefreshCw className="size-4" />
            {tc("retry")}
          </button>
        </div>
      )}

      {!loading && !error && rows.length === 0 && (
        <div className="rounded-2xl border border-border bg-card px-6 py-14 text-center">
          <span className="mx-auto flex size-12 items-center justify-center rounded-xl bg-muted text-muted-foreground">
            <Puzzle className="size-6" />
          </span>
          <p className="mt-4 text-sm font-medium text-foreground">{t("services.noServices")}</p>
          <p className="mt-1 text-xs text-muted-foreground">{t("services.noServicesHint")}</p>
          <button
            type="button"
            onClick={load}
            className="mt-5 inline-flex h-9 items-center gap-2 rounded-lg border border-border px-4 text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground"
          >
            <RefreshCw className="size-4" />
            {tc("retry")}
          </button>
        </div>
      )}

      {!loading && !error && rows.length > 0 && (
        <div className="overflow-hidden rounded-2xl border border-border bg-card shadow-xs">
          <div className="grid grid-cols-[1fr_auto] items-center gap-4 border-b border-border px-5 py-3 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
            <span>{t("services.serviceName")}</span>
            <span>{t("services.state")}</span>
          </div>

          <ul className="divide-y divide-border">
            {rows.map((row) => (
              <li
                key={row.key}
                className="grid grid-cols-[1fr_auto] items-center gap-4 px-5 py-4"
              >
                <div className="flex min-w-0 items-start gap-3">
                  <span
                    className={cn(
                      "mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg",
                      row.enabled ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground",
                    )}
                  >
                    <Settings className="size-4" />
                  </span>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-foreground">{row.name}</p>
                    {row.description && (
                      <p className="mt-0.5 line-clamp-2 text-xs text-muted-foreground">
                        {row.description}
                      </p>
                    )}
                    <p className="mt-1 text-[11px] uppercase tracking-wide text-muted-foreground">
                      {row.key}
                      {row.enabledAt
                        ? ` · ${t("services.enabledAt", { date: new Date(row.enabledAt).toLocaleDateString() })}`
                        : ""}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <span
                    className={cn(
                      "inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-semibold",
                      row.enabled
                        ? "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                        : "bg-muted text-muted-foreground",
                    )}
                  >
                    {row.enabled ? t("services.enabled") : t("services.disabled")}
                  </span>
                  <button
                    type="button"
                    role="switch"
                    aria-checked={row.enabled}
                    aria-label={`${row.enabled ? t("services.disable") : t("services.enable")} ${row.name}`}
                    disabled={savingKey !== null}
                    onClick={() => toggleService(row)}
                    className={cn(
                      "relative h-6 w-11 shrink-0 rounded-full transition-colors disabled:opacity-60",
                      row.enabled ? "bg-primary" : "bg-muted-foreground/30",
                    )}
                  >
                    <span
                      className={cn(
                        "absolute top-0.5 size-5 rounded-full bg-white shadow transition-transform",
                        row.enabled ? "left-[22px]" : "left-0.5",
                      )}
                    />
                    {savingKey === row.key && (
                      <Loader2 className="absolute -right-5 top-1.5 size-3.5 animate-spin text-muted-foreground" />
                    )}
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
