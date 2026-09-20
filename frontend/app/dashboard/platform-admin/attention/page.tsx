"use client"

import { useEffect, useState } from "react"
import { AlertTriangle, Loader2, ExternalLink } from "lucide-react"
import { platformAdminApi, type AttentionItem } from "@/lib/platform-admin-api"

export default function AttentionCenterPage() {
  const [items, setItems] = useState<AttentionItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await platformAdminApi.getAttention()
      setItems(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load attention items")
    } finally {
      setLoading(false)
    }
  }

  const getSeverityStyle = (severity: string) => {
    switch (severity) {
      case "HIGH":
        return "border-l-red-500 bg-red-500/5"
      case "MEDIUM":
        return "border-l-amber-500 bg-amber-500/5"
      default:
        return "border-l-blue-500 bg-blue-500/5"
    }
  }

  const getSeverityBadge = (severity: string) => {
    switch (severity) {
      case "HIGH":
        return <span className="inline-block rounded-full bg-red-500/10 px-2.5 py-0.5 text-xs font-medium text-red-600">HIGH</span>
      case "MEDIUM":
        return <span className="inline-block rounded-full bg-amber-500/10 px-2.5 py-0.5 text-xs font-medium text-amber-600">MEDIUM</span>
      default:
        return <span className="inline-block rounded-full bg-blue-500/10 px-2.5 py-0.5 text-xs font-medium text-blue-600">INFO</span>
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Attention Center</h1>
        <p className="mt-1 text-sm text-muted-foreground">Items requiring your attention across the platform.</p>
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
          <AlertTriangle className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">All clear</p>
          <p className="mt-1 text-sm text-muted-foreground">No items requiring attention right now.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {items.map((item, i) => (
            <a
              key={i}
              href={item.actionUrl}
              target="_blank"
              rel="noopener noreferrer"
              className={`block rounded-2xl border border-border border-l-4 bg-card p-5 shadow-xs hover:shadow-md transition-shadow ${getSeverityStyle(item.severity)}`}
            >
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    {getSeverityBadge(item.severity)}
                    <span className="text-xs font-medium text-muted-foreground">{item.category}</span>
                  </div>
                  <h3 className="mt-2 text-sm font-semibold text-foreground">{item.title}</h3>
                  <p className="mt-1 text-sm text-muted-foreground">{item.description}</p>
                </div>
                <ExternalLink className="size-4 shrink-0 text-muted-foreground" />
              </div>
            </a>
          ))}
        </div>
      )}
    </div>
  )
}
