"use client"

import { useCallback, useEffect, useState } from "react"
import { AlertTriangle, Database } from "lucide-react"
import { useTranslations } from "next-intl"
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

function IssueCard({ issue, t }: { issue: DataQualityIssue; t: ReturnType<typeof useTranslations> }) {
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
          <span className="font-semibold text-foreground">{t("dataQuality.suggestedActionLabel")}</span>{" "}
          {issue.suggestedAction}
        </p>
      )}
    </article>
  )
}

export default function DataQualityPage() {
  const t = useTranslations("regionalAdmin")
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
      setError(e instanceof Error ? e.message : t("dataQuality.loadError"))
    } finally {
      setLoading(false)
    }
  }, [t])

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
        title={t("dataQuality.title")}
        description={t("dataQuality.description")}
      />

      {error && <ErrorState message={error} onRetry={load} />}

      {loading && <LoadingState label={t("dataQuality.loadingLabel")} />}

      {!loading && !error && data && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label={t("dataQuality.statTotal")} value={data.totalIssues} />
            <StatCard label={t("dataQuality.statCritical")} value={data.criticalIssues} tone="danger" />
            <StatCard label={t("dataQuality.statWarnings")} value={data.warningIssues} tone="warning" />
            <StatCard label={t("dataQuality.statInfo")} value={data.infoIssues} />
          </div>

          {data.issues.length === 0 ? (
            <EmptyState
              title={t("dataQuality.emptyTitle")}
              hint={t("dataQuality.emptyHint")}
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
                    title={t("dataQuality.severityFindingsTitle", { severity })}
                    actions={<Chip tone={severityTone(severity)}>{issues.length}</Chip>}
                  >
                    <div className="space-y-3">
                      {issues.map((issue) => (
                        <IssueCard key={issue.id} issue={issue} t={t} />
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
              {t("dataQuality.footnote")}
            </p>
          )}
        </>
      )}
    </div>
  )
}
