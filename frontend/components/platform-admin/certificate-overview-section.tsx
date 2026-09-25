"use client"

import { useEffect, useState } from "react"
import { Award, Loader2, AlertCircle, ShieldCheck, Ban, FileEdit, QrCode, BarChart3 } from "lucide-react"
import { platformAdminApi, type CertificateOverview } from "@/lib/platform-admin-api"

/**
 * Platform certificate overview — renders only backend-computed aggregates.
 * When the endpoint fails, the panel states "Data unavailable" instead of inventing numbers.
 */
export function CertificateOverviewSection() {
  const [overview, setOverview] = useState<CertificateOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        setLoading(true)
        setError(null)
        const res = await platformAdminApi.getCertificateOverview()
        if (!cancelled) setOverview(res)
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Failed to load overview")
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error || !overview) {
    return (
      <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive flex items-center gap-2">
        <AlertCircle className="size-4" />
        {error || "Data unavailable"}
      </div>
    )
  }

  const cards = [
    { label: "Total certificates", value: overview.total, icon: Award, tone: "text-primary" },
    { label: "Issued", value: overview.issued, icon: ShieldCheck, tone: "text-green-600" },
    { label: "Revoked", value: overview.revoked, icon: Ban, tone: "text-red-600" },
    { label: "Draft", value: overview.draft, icon: FileEdit, tone: "text-muted-foreground" },
  ]

  const typeEntries = Object.entries(overview.byType || {})
  const maxType = Math.max(1, ...typeEntries.map(([, count]) => count))

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {cards.map((card) => (
          <div key={card.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center justify-between">
              <p className="text-xs font-medium text-muted-foreground">{card.label}</p>
              <card.icon className={`size-4 ${card.tone}`} />
            </div>
            <p className="mt-2 text-3xl font-bold tracking-tight text-foreground">{card.value}</p>
          </div>
        ))}
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs lg:col-span-2">
          <div className="mb-4 flex items-center gap-2">
            <BarChart3 className="size-4 text-primary" />
            <h3 className="text-sm font-semibold text-foreground">Certificates by type</h3>
          </div>
          {typeEntries.length === 0 || typeEntries.every(([, count]) => count === 0) ? (
            <p className="rounded-xl border border-dashed border-border px-4 py-6 text-center text-sm text-muted-foreground">
              No certificates have been issued yet.
            </p>
          ) : (
            <ul className="space-y-3">
              {typeEntries.map(([type, count]) => (
                <li key={type}>
                  <div className="mb-1 flex items-center justify-between text-xs">
                    <span className="font-medium text-foreground">{type}</span>
                    <span className="text-muted-foreground">{count}</span>
                  </div>
                  <div className="h-2 rounded-full bg-muted">
                    <div
                      className="h-2 rounded-full bg-primary"
                      style={{ width: `${Math.round((count / maxType) * 100)}%` }}
                    />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="mb-3 flex items-center gap-2">
            <QrCode className="size-4 text-teal" />
            <h3 className="text-sm font-semibold text-foreground">Verification activity</h3>
          </div>
          <p className="text-3xl font-bold tracking-tight text-foreground">{overview.verificationActivity30Days}</p>
          <p className="mt-1 text-xs text-muted-foreground">
            Public verification checks recorded in the last 30 days.
          </p>
          <p className="mt-4 text-[11px] text-muted-foreground">
            Counts update {new Date(overview.generatedAt).toLocaleString("en-GB", { dateStyle: "medium", timeStyle: "short" })}.
          </p>
        </div>
      </div>
    </div>
  )
}
