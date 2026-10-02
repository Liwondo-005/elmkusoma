"use client"

import { useEffect, useState, useCallback, type ReactNode } from "react"
import {
  Loader2, Inbox, XCircle, RefreshCw, Search, ChevronLeft,
} from "lucide-react"
import { useTranslations } from "next-intl"
import { cn } from "@/lib/utils"
import type { PageResponse } from "@/lib/regional-admin-api"

export function PageHeader({
  title, description, actions,
}: { title: string; description?: string; actions?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{title}</h1>
        {description && <p className="mt-1 max-w-3xl text-sm text-muted-foreground">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  )
}

export function Panel({ title, actions, children, className }: {
  title?: string; actions?: ReactNode; children: ReactNode; className?: string
}) {
  return (
    <section className={cn("rounded-2xl border border-border bg-card p-5 shadow-sm", className)}>
      {title && (
        <div className="mb-4 flex items-center justify-between gap-3">
          <h2 className="text-sm font-bold text-foreground">{title}</h2>
          {actions}
        </div>
      )}
      {children}
    </section>
  )
}

export function StatCard({ label, value, hint, tone = "default" }: {
  label: string; value: string | number; hint?: string;
  tone?: "default" | "success" | "warning" | "danger"
}) {
  const tones = {
    default: "text-foreground",
    success: "text-emerald-600",
    warning: "text-amber-600",
    danger: "text-red-600",
  }
  return (
    <div className="rounded-2xl border border-border bg-card p-4 shadow-sm">
      <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{label}</p>
      <p className={cn("mt-2 text-2xl font-bold tabular-nums", tones[tone])}>
        {typeof value === "number" ? value.toLocaleString() : value}
      </p>
      {hint && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
    </div>
  )
}

export function LoadingState({ label }: { label?: string }) {
  const t = useTranslations("regionalAdmin")
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-muted-foreground">
      <Loader2 className="size-8 animate-spin" />
      <p className="text-sm">{label ?? t("shared.loading")}</p>
    </div>
  )
}

export function EmptyState({ title, hint, icon }: { title: string; hint?: string; icon?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-16 text-center">
      <span className="text-muted-foreground/50">{icon ?? <Inbox className="size-10" />}</span>
      <p className="mt-4 text-sm font-medium text-foreground">{title}</p>
      {hint && <p className="mt-1 max-w-md text-sm text-muted-foreground">{hint}</p>}
    </div>
  )
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  const tc = useTranslations("common")
  return (
    <div className="flex items-center justify-between rounded-2xl border border-destructive/20 bg-destructive/5 px-5 py-4 text-sm text-destructive">
      <span className="flex items-center gap-2">
        <XCircle className="size-4 shrink-0" /> {message}
      </span>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="inline-flex items-center gap-1 rounded-lg border border-destructive/30 bg-background px-3 py-1 text-xs font-semibold"
        >
          <RefreshCw className="size-3" /> {tc("retry")}
        </button>
      )}
    </div>
  )
}

export function Chip({ children, tone = "default" }: {
  children: ReactNode
  tone?: "default" | "success" | "warning" | "danger" | "info" | "muted"
}) {
  const tones = {
    default: "bg-primary/10 text-primary",
    success: "bg-emerald-500/10 text-emerald-600",
    warning: "bg-amber-500/10 text-amber-600",
    danger: "bg-red-500/10 text-red-600",
    info: "bg-blue-500/10 text-blue-600",
    muted: "bg-muted text-muted-foreground",
  }
  return (
    <span className={cn("inline-block rounded-full px-2.5 py-0.5 text-xs font-medium", tones[tone])}>
      {children}
    </span>
  )
}

export function ProgressBar({ value }: { value: number | null | undefined }) {
  const safe = value === null || value === undefined ? 0 : Math.max(0, Math.min(100, value))
  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-muted">
        <div className="h-full rounded-full bg-primary" style={{ width: `${safe}%` }} />
      </div>
      <span className="w-12 text-right text-xs tabular-nums text-muted-foreground">
        {value === null || value === undefined ? "—" : `${value.toFixed(1)}%`}
      </span>
    </div>
  )
}

export function Percent({ value }: { value: number | null | undefined }) {
  if (value === null || value === undefined) return <span className="text-muted-foreground">—</span>
  return <span className="tabular-nums">{value.toFixed(1)}%</span>
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return "—"
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? "—" : d.toLocaleDateString()
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—"
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? "—" : d.toLocaleString()
}

/** Generic search + pagination container used by every Regional Admin list page. */
export function PagedList<T>({
  fetcher, renderItem, searchPlaceholder, emptyTitle, emptyHint, emptyIcon, pageSize = 20, toolbar,
}: {
  fetcher: (params: { page: number; size: number; search?: string }) => Promise<PageResponse<T>>
  renderItem: (item: T) => ReactNode
  searchPlaceholder: string
  emptyTitle: string
  emptyHint?: string
  emptyIcon?: ReactNode
  pageSize?: number
  toolbar?: ReactNode
}) {
  const [data, setData] = useState<PageResponse<T> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const t = useTranslations("regionalAdmin")
  const tc = useTranslations("common")
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState("")
  const [input, setInput] = useState("")

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetcher({ page, size: pageSize, search: search || undefined })
      setData(res)
    } catch (e) {
      setError(e instanceof Error ? e.message : t("shared.loadError"))
    } finally {
      setLoading(false)
    }
  }, [fetcher, page, pageSize, search])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <form
          onSubmit={(e) => { e.preventDefault(); setPage(0); setSearch(input.trim()) }}
          className="relative min-w-64 flex-1"
          role="search"
        >
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="search"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder={searchPlaceholder}
            aria-label={searchPlaceholder}
            className="w-full rounded-lg border border-border bg-background py-2.5 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-ring"
          />
        </form>
        {toolbar}
      </div>

      {error && <ErrorState message={error} onRetry={load} />}

      {loading ? (
        <LoadingState />
      ) : !data || data.content.length === 0 ? (
        <EmptyState title={emptyTitle} hint={emptyHint} icon={emptyIcon} />
      ) : (
        <div className="space-y-3">{data.content.map((item, i) => (
          <div key={(item as { id?: string }).id ?? i}>{renderItem(item)}</div>
        ))}</div>
      )}

      {data && data.totalElements > pageSize && (
        <div className="flex items-center justify-center gap-3">
          <button
            type="button"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            <ChevronLeft className="size-4" /> {tc("previous")}
          </button>
          <span className="text-sm text-muted-foreground">
            {t("shared.pageStatus", { page: page + 1, pages: Math.max(1, data.totalPages), total: data.totalElements.toLocaleString() })}
          </span>
          <button
            type="button"
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={page >= data.totalPages - 1}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            {tc("next")}
          </button>
        </div>
      )}
    </div>
  )
}
