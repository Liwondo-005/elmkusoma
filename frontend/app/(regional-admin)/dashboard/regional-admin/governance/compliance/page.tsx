"use client"

import { useCallback, useEffect, useState } from "react"
import { CheckCircle2, ShieldCheck, XCircle } from "lucide-react"
import { useTranslations } from "next-intl"
import {
  regionalAdminApi,
  type ComplianceCheck,
  type ComplianceResponse,
} from "@/lib/regional-admin-api"
import {
  Chip,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Panel,
  ProgressBar,
  StatCard,
  formatDateTime,
} from "@/components/dashboard/regional-admin/ui"

function CheckRow({
  check, t,
}: {
  check: ComplianceCheck
  t: ReturnType<typeof useTranslations>
}) {
  return (
    <li className="rounded-xl border border-border bg-card p-4 shadow-sm">
      <div className="flex items-start gap-3">
        {check.passed ? (
          <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-emerald-600" />
        ) : (
          <XCircle className="mt-0.5 size-5 shrink-0 text-red-600" />
        )}
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="text-sm font-bold text-foreground">{check.name}</h3>
            <Chip tone="muted">{check.category}</Chip>
            <Chip tone={check.passed ? "success" : "danger"}>
              {check.passed ? t("compliance.passedBadge") : t("compliance.failedBadge")}
            </Chip>
          </div>
          <p className="mt-1 text-sm leading-relaxed text-muted-foreground">{check.description}</p>
          {!check.passed && check.remediation && (
            <p className="mt-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
              <span className="font-semibold">{t("compliance.remediationLabel")}</span> {check.remediation}
            </p>
          )}
          <p className="mt-2 text-xs text-muted-foreground">
            {t("compliance.lastCheckedLabel")}: {formatDateTime(check.lastChecked)}
          </p>
        </div>
      </div>
    </li>
  )
}

export default function CompliancePage() {
  const t = useTranslations("regionalAdmin")
  const [data, setData] = useState<ComplianceResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setData(await regionalAdminApi.getCompliance())
    } catch (e) {
      setData(null)
      setError(e instanceof Error ? e.message : t("compliance.loadError"))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    load()
  }, [load])

  const passPercent =
    data && data.totalChecks > 0 ? (data.passedChecks / data.totalChecks) * 100 : null

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title={t("compliance.title")}
        description={t("compliance.description")}
      />

      {data && data.note && (
        <div className="rounded-xl border border-border bg-muted/50 px-4 py-3 text-sm leading-relaxed text-muted-foreground">
          {data.note}
        </div>
      )}

      {error && <ErrorState message={error} onRetry={load} />}

      {loading && <LoadingState label={t("compliance.loadingLabel")} />}

      {!loading && !error && data && (
        <>
          <Panel
            title={t("compliance.summaryTitle")}
            actions={
              <Chip tone={data.isCompliant ? "success" : "danger"}>
                {data.isCompliant ? t("compliance.compliantBadge") : t("compliance.notCompliantBadge")}
              </Chip>
            }
          >
            <div className="grid gap-4 sm:grid-cols-3">
              <StatCard label={t("compliance.statPassed")} value={data.passedChecks} tone="success" />
              <StatCard label={t("compliance.statFailed")} value={data.failedChecks} tone="danger" />
              <StatCard label={t("compliance.statTotal")} value={data.totalChecks} />
            </div>
            <div className="mt-4">
              <ProgressBar value={passPercent} />
            </div>
          </Panel>

          {data.checks.length === 0 ? (
            <EmptyState
              title={t("compliance.emptyTitle")}
              icon={<ShieldCheck className="size-10" />}
            />
          ) : (
            <Panel title={t("compliance.checksTitle", { count: data.checks.length })}>
              <ul className="space-y-3">
                {data.checks.map((check) => (
                  <CheckRow key={check.id} check={check} t={t} />
                ))}
              </ul>
            </Panel>
          )}
        </>
      )}
    </div>
  )
}
