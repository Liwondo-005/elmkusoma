"use client"

import { useCallback, useEffect, useState } from "react"
import { AlertTriangle, Database } from "lucide-react"
import {
  regionalAdminApi,
  type DataQualityIssue,
  type DataQualityResponse,
} from "@/lib/regional-admin-api"
import {
  Chip,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Panel,
  StatCard,
  formatDateTime,
} from "@/components/dashboard/regional-admin/ui"

const SEVERITY_ORDER = ["CRITICAL", "HIGH", "WARNING", "INFO"]

function severityTone(severity: string): "danger" | "warning" | "info" | "muted" {
  if (severity === "CRITICAL" || severity === "HIGH") return "danger"
  if (severity === "WARNING") return "warning"
  if (severity === "INFO") return "info"
  return "muted"
}

function IssueCard({ issue }: { issue: DataQualityIssue }) {
  return (
    <article className="rounded-xl border border-border bg-card p-4 shadow-sm">
      <div className="flex flex-wrap items-center gap-2">
        <Chip tone={severityTone(issue.severity)}>{issue.severity}</Chip>
        <h3 className="text-sm font-bold text-foreground">{issue.title}</h3>
      </div>
      <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">{issue.description}</p>
      <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
        <Chip tone="muted">{issue.entityType}</Chip>
        <span className="text-foreground">{issue.entityName}</span>
        <span aria-hidden="true">·</span>
        <span>{formatDateTime(issue.detectedAt)}</span>
      </div>
      {issue.suggestedAction && (
        <p className="mt-2 rounded-lg border border-border bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
          <span className="font-semibold text-foreground">Suggested action:</span>{" "}
          {issue.suggestedAction}
        </p>
      )}
    </article>
  )
}

export default function DataQualityPage() {
  const [data, setData] = useState<DataQualityResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setData(await regionalAdminApi.getDataQuality())
    } catch (e) {
      setData(null)
      setError(e instanceof Error ? e.message : "Unable to load data quality findings.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const severities = (() => {
    const found = new Set((data?.issues ?? []).map((i) => i.severity))
    return [
      ...SEVERITY_ORDER.filter((s) => found.has(s)),
      ...Array.from(found).filter((s) => !SEVERITY_ORDER.includes(s)),
    ]
  })()

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Data Quality"
        description="Real data-quality findings detected in your jurisdiction (missing links, duplicates, gaps)."
      />

      {error && <ErrorState message={error} onRetry={load} />}

      {loading && <LoadingState label="Loading data quality findings…" />}

      {!loading && !error && data && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Total issues" value={data.totalIssues} />
            <StatCard label="Critical" value={data.criticalIssues} tone="danger" />
            <StatCard label="Warnings" value={data.warningIssues} tone="warning" />
            <StatCard label="Info" value={data.infoIssues} />
          </div>

          {data.issues.length === 0 ? (
            <EmptyState
              title="No data quality findings detected in your jurisdiction."
              hint="Missing links, duplicates and record gaps will appear here as soon as the platform detects them."
              icon={<Database className="size-10" />}
            />
          ) : (
            <div className="space-y-6">
              {severities.map((severity) => {
                const issues = data.issues.filter((i) => i.severity === severity)
                if (issues.length === 0) return null
                return (
                  <Panel
                    key={severity}
                    title={`${severity} findings`}
                    actions={<Chip tone={severityTone(severity)}>{issues.length}</Chip>}
                  >
                    <div className="space-y-3">
                      {issues.map((issue) => (
                        <IssueCard key={issue.id} issue={issue} />
                      ))}
                    </div>
                  </Panel>
                )
              })}
            </div>
          )}

          {data.issues.length > 0 && (
            <p className="flex items-center gap-2 text-xs text-muted-foreground">
              <AlertTriangle className="size-3.5" />
              Every finding above was detected from stored records inside your jurisdiction.
            </p>
          )}
        </>
      )}
    </div>
  )
}
