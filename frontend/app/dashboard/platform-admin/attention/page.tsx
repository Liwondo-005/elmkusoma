"use client"

import { useEffect, useState } from "react"
import { AlertTriangle, Loader2, ExternalLink, Shield, Clock, CheckCircle2 } from "lucide-react"
import { platformAdminApi, type AttentionItem, type IncidentSummary, type VerificationSummary, type SecurityEventItem } from "@/lib/platform-admin-api"

interface EnrichedAttention {
  severity: string; title: string; description: string; category: string; actionUrl: string
}

export default function AttentionCenterPage() {
  const [items, setItems] = useState<EnrichedAttention[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => { loadData() }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      setError(null)
      const [attention, incidents, verifications, security] = await Promise.allSettled([
        platformAdminApi.getAttention(),
        platformAdminApi.listIncidents(0, 10, "DETECTED"),
        platformAdminApi.listPendingVerifications(),
        platformAdminApi.getSecurityEvents(),
      ])

      const all: EnrichedAttention[] = []

      if (attention.status === "fulfilled") all.push(...attention.value)

      if (incidents.status === "fulfilled") {
        incidents.value.content.forEach((inc: IncidentSummary) => {
          all.push({
            severity: inc.severity,
            title: inc.title,
            description: inc.description || `${inc.category} incident — ${inc.status}`,
            category: "INCIDENT",
            actionUrl: "/dashboard/platform-admin/incidents",
          })
        })
      }

      if (verifications.status === "fulfilled") {
        verifications.value.forEach((v: VerificationSummary) => {
          all.push({
            severity: "MEDIUM",
            title: `Pending Verification: ${v.verificationType}`,
            description: `${v.entityType} submitted for review`,
            category: "VERIFICATION",
            actionUrl: "/dashboard/platform-admin/verifications",
          })
        })
      }

      if (security.status === "fulfilled") {
        security.value.forEach((e: SecurityEventItem) => {
          all.push({
            severity: e.severity || "MEDIUM",
            title: `Security: ${e.eventType}`,
            description: e.description || "Security event requires review",
            category: "SECURITY",
            actionUrl: "/dashboard/platform-admin/security",
          })
        })
      }

      all.sort((a, b) => {
        const order: Record<string, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3, INFO: 4 }
        return (order[a.severity] ?? 5) - (order[b.severity] ?? 5)
      })

      setItems(all)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load attention items")
    } finally {
      setLoading(false)
    }
  }

  const getSeverityStyle = (severity: string) => {
    switch (severity) {
      case "CRITICAL": return "border-l-red-600 bg-red-600/5"
      case "HIGH": return "border-l-red-500 bg-red-500/5"
      case "MEDIUM": return "border-l-amber-500 bg-amber-500/5"
      default: return "border-l-blue-500 bg-blue-500/5"
    }
  }

  const getSeverityBadge = (severity: string) => {
    const styles: Record<string, string> = {
      CRITICAL: "bg-red-500/10 text-red-700", HIGH: "bg-red-500/10 text-red-600",
      MEDIUM: "bg-amber-500/10 text-amber-600", LOW: "bg-blue-500/10 text-blue-600", INFO: "bg-blue-500/10 text-blue-600",
    }
    return <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${styles[severity] || styles.INFO}`}>{severity}</span>
  }

  const categoryIcons: Record<string, typeof Shield> = { SECURITY: Shield, INCIDENT: AlertTriangle, VERIFICATION: CheckCircle2, LIVE: Clock }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Attention Center</h1>
          <p className="mt-1 text-sm text-muted-foreground">Prioritized items requiring admin action across the platform</p>
        </div>
        <button onClick={loadData} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-4 py-2 text-sm font-medium text-foreground hover:bg-muted">
          Refresh
        </button>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        </div>
      ) : items.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <CheckCircle2 className="size-10 text-green-500" />
          <p className="mt-4 text-sm font-medium text-foreground">All Clear</p>
          <p className="mt-1 text-sm text-muted-foreground">No items requiring attention right now.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {items.map((item, i) => {
            const Icon = categoryIcons[item.category] || AlertTriangle
            return (
              <a
                key={i}
                href={item.actionUrl}
                className={`block rounded-2xl border border-border border-l-4 bg-card p-5 shadow-xs hover:shadow-md transition-shadow ${getSeverityStyle(item.severity)}`}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex items-start gap-3 min-w-0">
                    <Icon className="size-5 shrink-0 mt-0.5 text-muted-foreground" />
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        {getSeverityBadge(item.severity)}
                        <span className="text-xs font-medium text-muted-foreground">{item.category}</span>
                      </div>
                      <h3 className="mt-2 text-sm font-semibold text-foreground">{item.title}</h3>
                      <p className="mt-1 text-sm text-muted-foreground">{item.description}</p>
                    </div>
                  </div>
                  <ExternalLink className="size-4 shrink-0 text-muted-foreground" />
                </div>
              </a>
            )
          })}
        </div>
      )}
    </div>
  )
}
